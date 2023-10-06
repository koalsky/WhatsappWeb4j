package it.auties.whatsapp.socket;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.Proxy.Type;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_ENDPOINT;
import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_PORT;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    final URI proxy;
    final Executor executor;
    final ReentrantLock outputLock;
    SocketListener listener;
    private SocketSession(URI proxy, Executor executor) {
        this.proxy = proxy;
        this.executor = executor;
        this.outputLock = new ReentrantLock(true);
    }

    abstract CompletableFuture<Void> connect(SocketListener listener);
    abstract void disconnect();
    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);
    abstract boolean isOpen();

    static SocketSession of(URI proxy, Executor executor, boolean webSocket){
        if(webSocket) {
            return new WebSocketSession(proxy, executor);
        }

        return new RawSocketSession(proxy, executor);
    }

    Proxy getProxy() {
        if (proxy == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(proxy.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(proxy));
        var host = Objects.requireNonNull(proxy.getHost(), "Invalid proxy, expected a host: %s".formatted(proxy));
        var port = getProxyPort(scheme).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(proxy)));
        return switch (scheme) {
            case "http", "https" -> new Proxy(Type.HTTP, new InetSocketAddress(host, port));
            case "socks4", "socks5" -> new Proxy(Type.SOCKS, new InetSocketAddress(host, port));
            default -> throw new IllegalStateException("Unexpected scheme: " + scheme);
        };
    }

    private OptionalInt getProxyPort(String scheme) {
        return proxy.getPort() != -1 ? OptionalInt.of(proxy.getPort()) : switch (scheme) {
            case "http" -> OptionalInt.of(80);
            case "https" -> OptionalInt.of(443);
            default -> OptionalInt.empty();
        };
    }


    int decodeLength(ByteBuf buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    public static final class WebSocketSession extends SocketSession implements WebSocket.Listener {
        private final Collection<ByteBuffer> inputParts;
        private WebSocket session;

        WebSocketSession(URI proxy, Executor executor) {
            super(proxy, executor);
            this.inputParts = new ConcurrentLinkedDeque<>();
        }

        @SuppressWarnings("resource") // Not needed
        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if(isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            this.listener = listener;
            return HttpClient.newBuilder()
                    .executor(executor)
                    .proxy(ProxySelector.of((InetSocketAddress) getProxy().address()))
                    .authenticator(new ProxyAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(Specification.Whatsapp.WEB_SOCKET_ENDPOINT, this)
                    .thenRun(() -> listener.onOpen(this));
        }

        @Override
        void disconnect() {
            if(!isOpen()) {
                return;
            }

            session.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            outputLock.lock();
            return session.sendBinary(ByteBuffer.wrap(bytes), true)
                    .thenRun(outputLock::unlock)
                    .exceptionally(exception -> {
                        outputLock.unlock();
                        throw new RequestException(exception);
                    });
        }

        @Override
        boolean isOpen() {
            return session != null && !session.isInputClosed() && !session.isOutputClosed();
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.session = webSocket;
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            inputParts.clear();
            listener.onClose();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            var buffer = getRequestBuffer(data, last);
            if (buffer.isEmpty()) {
                inputParts.add(data);
                return WebSocket.Listener.super.onBinary(webSocket, data, last);
            }

            while (buffer.get().readableBytes() >= 3) {
                var length = decodeLength(buffer.get());
                if (length < 0) {
                    break;
                }

                var result = buffer.get().readBytes(length);
                try {
                    listener.onMessage(BytesHelper.readBuffer(result));
                }catch (Throwable throwable) {
                    listener.onError(throwable);
                }

                result.release();
            }

            buffer.get().release();
            return WebSocket.Listener.super.onBinary(webSocket, data, true);
        }

        private Optional<ByteBuf> getRequestBuffer(ByteBuffer data, boolean last) {
            if (!last) {
                inputParts.add(data);
                return Optional.empty();
            }

            if (inputParts.isEmpty()) {
                return Optional.of(BytesHelper.newBuffer(data));
            }

            inputParts.add(data);
            var result = Optional.of(BytesHelper.newBuffer(inputParts));
            inputParts.clear();
            return result;
        }
    }

    static final class RawSocketSession extends SocketSession {
        static {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        // TODO: Explore AsynchronousSocketChannel
        private Socket socket;
        private boolean closed;

        RawSocketSession(URI proxy, Executor executor) {
            super(proxy, executor);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    this.listener = listener;
                    this.socket = new Socket(getProxy());
                    socket.setKeepAlive(true);
                    socket.connect(new InetSocketAddress(SOCKET_ENDPOINT, SOCKET_PORT));
                    executor.execute(this::readMessages);
                    this.closed = false;
                    listener.onOpen(this);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot connect to host", exception);
                }
            }, executor);
        }

        @Override
        @SuppressWarnings("UnusedReturnValue")
        void disconnect() {
            if (!isOpen()) {
                return;
            }

            try {
                socket.close();
                closeResources();
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot close connection to host", exception);
            }
        }

        @Override
        public boolean isOpen() {
            return socket != null && socket.isConnected();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            return CompletableFuture.runAsync(() -> {
                try {
                    outputLock.lock();
                    if (socket == null) {
                        return;
                    }
                    var stream = socket.getOutputStream();
                    stream.write(bytes);
                    stream.flush();
                }catch (SocketException exception) {
                    closeResources();
                } catch (IOException exception) {
                    throw new RequestException(exception);
                }finally {
                    outputLock.unlock();
                }
            }, executor);
        }

        private void readMessages() {
            try (var input = new DataInputStream(socket.getInputStream())) {
                while (isOpen()) {
                    var length = decodeLength(input);
                    if (length < 0) {
                        break;
                    }
                    var message = new byte[length];
                    if(isOpen()) {
                        input.readFully(message);
                    }
                    listener.onMessage(message);
                }
            } catch(Throwable throwable) {
                listener.onError(throwable);
            } finally {
                closeResources();
            }
        }

        private int decodeLength(DataInputStream input) {
            try {
                var lengthBytes = new byte[3];
                input.readFully(lengthBytes);
                return decodeLength(BytesHelper.newBuffer(lengthBytes));
            } catch (IOException exception) {
                return -1;
            }
        }

        private void closeResources() {
            if(closed) {
                return;
            }

            this.closed = true;
            this.socket = null;
            if (executor instanceof ExecutorService service) {
                service.shutdownNow();
            }

            listener.onClose();
        }
    }
}