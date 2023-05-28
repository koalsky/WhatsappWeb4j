package it.auties.whatsapp;

import it.auties.whatsapp.api.HistoryLength;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.util.JacksonProvider;

import java.nio.file.Path;

// Just used for testing locally
public class WaitTest implements JacksonProvider {
    public static void main(String[] args) {

        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "12307");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "12307");


//    Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        WebOptions webOptions = WebOptions.builder().name("木辛AI智能机器人").historyLength(HistoryLength.THREE_MONTHS).qrHandler(QrHandler.toFile(QrHandler.ToFileConsumer.toDesktop())).build();
        var whatsapp = Whatsapp.newConnection(webOptions)
                .addLoggedInListener(() -> System.out.println("连接成功"))
                .addNewMessageListener(message -> System.out.println(message.toJson()))
                .addContactsListener(
                        (api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener(
                        (action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addContactPresenceListener(
                        (chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n",
                                contact.name(), chat.name(), status.name()))
                .addAnyMessageStatusListener((chat, contact, info, status) -> System.out.printf(
                        "Message %s in chat %s now has status %s for %s %n", info.id(), info.chatName(), status,
                        contact == null ? null : contact.name()))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason));
        whatsapp.connect().join();
    }
}
