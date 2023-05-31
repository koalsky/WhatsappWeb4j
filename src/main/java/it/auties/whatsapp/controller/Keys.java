package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.RegistrationStatus;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.sync.AppStateSyncKey;
import it.auties.whatsapp.model.sync.LTHashState;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.Spec;
import lombok.AccessLevel;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElseGet;

/**
 * This controller holds the cryptographic-related data regarding a WhatsappWeb session
 */
@SuperBuilder
@Jacksonized
@Accessors(fluent = true, chain = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Keys extends Controller<Keys> {
    /**
     * The client id
     */
    @Getter
    @Default
    private int registrationId = KeyHelper.registrationId();

    /**
     * The secret key pair used for buffer messages
     */
    @Default
    @NonNull
    @Getter
    private SignalKeyPair noiseKeyPair = SignalKeyPair.random();

    /**
     * The ephemeral key pair
     */
    @Default
    @NonNull
    @Getter
    private SignalKeyPair ephemeralKeyPair = SignalKeyPair.random();

    /**
     * The signed identity key
     */
    @Default
    @NonNull
    @Getter
    private SignalKeyPair identityKeyPair = SignalKeyPair.random();

    /**
     * The signed pre key
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private SignalSignedKeyPair signedKeyPair;

    /**
     * The signed key of the companion's device
     * This value will be null until it gets synced by whatsapp
     */
    @Getter
    @Setter
    private byte[] signedKeyIndex;

    /**
     * The timestamp of the signed key companion's device
     */
    @Getter
    @Setter
    private long signedKeyIndexTimestamp;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @Default
    @NonNull
    private ArrayList<SignalPreKeyPair> preKeys = new ArrayList<>();

    /**
     * The companion secret key
     */
    @Default
    @Getter
    private byte[] companionKey = SignalKeyPair.random().publicKey();

    /**
     * The prologue to send in a message
     */
    @Getter
    private byte @NonNull [] prologue;

    /**
     * The phone id for the mobile api
     */
    @Getter
    @Default
    private String phoneId = KeyHelper.phoneId();

    /**
     * The device id for the mobile api
     */
    @Getter
    @Default
    private String deviceId = KeyHelper.deviceId();

    /**
     * The identity id for the mobile api
     */
    @Getter
    @Default
    private String recoveryToken = KeyHelper.identityId();

    /**
     * The bytes of the encoded {@link SignedDeviceIdentityHMAC} received during the auth process
     */
    private SignedDeviceIdentity companionIdentity;

    /**
     * Sender keys for signal implementation
     */
    @NonNull
    @Default
    private Map<SenderKeyName, SenderKeyRecord> senderKeys = new ConcurrentHashMap<>();

    /**
     * App state keys
     */
    @NonNull
    @Default
    private Map<ContactJid, LinkedList<AppStateSyncKey>> appStateKeys = new ConcurrentHashMap<>();

    /**
     * Sessions map
     */
    @NonNull
    @Default
    @Getter
    private Map<SessionAddress, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Hash state
     */
    @NonNull
    @Default
    private Map<ContactJid, Map<PatchType, LTHashState>> hashStates = new ConcurrentHashMap<>();

    /**
     * Whether the client was registered, if mobile app
     */
    @Getter
    @Setter
    @Default
    private RegistrationStatus registrationStatus = RegistrationStatus.UNREGISTERED;

    /**
     * Write counter for IV
     */
    @NonNull
    @JsonIgnore
    @Default
    private AtomicLong writeCounter = new AtomicLong();

    /**
     * Read counter for IV
     */
    @NonNull
    @JsonIgnore
    @Default
    private AtomicLong readCounter = new AtomicLong();

    /**
     * Session dependent keys to write and read cyphered messages
     */
    @JsonIgnore
    @Getter
    @Setter
    private Bytes writeKey, readKey;

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @param uuid        the  uuid of the session, can be null
     * @param phoneNumber the phone number of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Optional<Keys> of(UUID uuid, Long phoneNumber, @NonNull ClientType clientType) {
        return of(uuid, phoneNumber, clientType, false);
    }

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @param uuid        the  uuid of the session, can be null
     * @param phoneNumber the phone number of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param required    whether an exception should be thrown if the connection doesn't exist
     * @return a non-null store
     */
    public static Optional<Keys> of(UUID uuid, Long phoneNumber, @NonNull ClientType clientType, boolean required) {
        return of(uuid, phoneNumber, clientType, DefaultControllerSerializer.instance(), required);
    }

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @param uuid        the non-null uuid of the session, can be null
     * @param phoneNumber the phone number of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer
     * @param required    whether an exception should be thrown if the connection doesn't exist
     * @return a non-null store
     */
    public static Optional<Keys> of(UUID uuid, Long phoneNumber, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer, boolean required) {
        if (uuid == null && phoneNumber == null && required) {
            return Optional.empty();
        }

        var id = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        var result = phoneNumber != null ? serializer.deserializeKeys(clientType, phoneNumber) : serializer.deserializeKeys(clientType, id);
        if (required && result.isEmpty()) {
            return Optional.empty();
        }

        return result.map(keys -> keys.serializer(serializer))
                .or(() -> Optional.of(random(id, phoneNumber, clientType, serializer)));
    }

    /**
     * Returns a new instance of random keys
     *
     * @param uuid       the uuid of the session to create, can be null
     * @param clientType the non-null type of the client
     * @return a non-null instance
     */
    public static Keys random(UUID uuid, Long phoneNumber, @NonNull ClientType clientType) {
        return random(uuid, phoneNumber, clientType, DefaultControllerSerializer.instance());
    }

    /**
     * Returns a new instance of random keys
     *
     * @param uuid       the uuid of the session to create, can be null
     * @param clientType the non-null type of the client
     * @param serializer the non-null serializer
     * @return a non-null instance
     */
    public static Keys random(UUID uuid, Long phoneNumber, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        var result = Keys.builder()
                .phoneNumber(PhoneNumber.ofNullable(phoneNumber).orElse(null))
                .serializer(serializer)
                .uuid(Objects.requireNonNullElseGet(uuid, UUID::randomUUID))
                .clientType(clientType)
                .prologue(clientType == ClientType.WEB ? Spec.Whatsapp.WEB_PROLOGUE : Spec.Whatsapp.APP_PROLOGUE)
                .build();
        result.signedKeyPair(SignalSignedKeyPair.of(result.registrationId(), result.identityKeyPair()));
        result.serialize(true);
        return result;
    }

    /**
     * Returns the encoded id
     *
     * @return a non-null byte array
     */
    public byte[] encodedRegistrationId() {
        return BytesHelper.intToBytes(registrationId(), 4);
    }

    /**
     * Clears the signal keys associated with this object
     */
    public void clearReadWriteKey() {
        this.writeKey = null;
        this.writeCounter.set(0);
        this.readCounter.set(0);
    }

    /**
     * Checks if the client sent pre keys to the server
     *
     * @return true if the client sent pre keys to the server
     */
    public boolean hasPreKeys() {
        return !preKeys.isEmpty();
    }

    /**
     * Queries the first {@link SenderKeyRecord} that matches {@code name}
     *
     * @param name the non-null name to search
     * @return a non-null SenderKeyRecord
     */
    public SenderKeyRecord findSenderKeyByName(@NonNull SenderKeyName name) {
        return requireNonNullElseGet(senderKeys.get(name), () -> {
            var record = new SenderKeyRecord();
            senderKeys.put(name, record);
            return record;
        });
    }

    /**
     * Queries the {@link Session} that matches {@code address}
     *
     * @param address the non-null address to search
     * @return a non-null Optional SessionRecord
     */
    public Optional<Session> findSessionByAddress(@NonNull SessionAddress address) {
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the id to search
     * @return a non-null signed key pair
     * @throws IllegalArgumentException if no element can be found
     */
    public Optional<SignalSignedKeyPair> findSignedKeyPairById(int id) {
        return id == signedKeyPair.id() ? Optional.of(signedKeyPair) : Optional.empty();
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the non-null id to search
     * @return a non-null pre key
     */
    public Optional<SignalPreKeyPair> findPreKeyById(Integer id) {
        return id == null ? Optional.empty() : preKeys.stream().filter(preKey -> preKey.id() == id).findFirst();
    }

    /**
     * Queries the app state key that matches {@code id}
     *
     * @param jid the non-null jid of the app key
     * @param id  the non-null id to search
     * @return a non-null Optional app state dataSync key
     */
    public Optional<AppStateSyncKey> findAppKeyById(@NonNull ContactJid jid, byte[] id) {
        return Objects.requireNonNull(appStateKeys.get(jid), "Missing keys")
                .stream()
                .filter(preKey -> preKey.keyId() != null && Arrays.equals(preKey.keyId().keyId(), id))
                .findFirst();
    }

    /**
     * Queries the hash state that matches {@code name}. Otherwise, creates a new one.
     *
     * @param device    the non-null device
     * @param patchType the non-null name to search
     * @return a non-null hash state
     */
    public Optional<LTHashState> findHashStateByName(@NonNull ContactJid device, @NonNull PatchType patchType) {
        return Optional.ofNullable(hashStates.get(device)).map(entry -> entry.get(patchType));
    }

    /**
     * Checks whether {@code identityKey} is trusted for {@code address}
     *
     * @param address     the non-null address
     * @param identityKey the nullable identity key
     * @return true if any match is found
     */
    public boolean hasTrust(@NonNull SessionAddress address, byte[] identityKey) {
        return true; // At least for now
    }

    /**
     * Checks whether a session already exists for the given address
     *
     * @param address the address to check
     * @return true if a session for that address already exists
     */
    public boolean hasSession(@NonNull SessionAddress address) {
        return sessions.containsKey(address);
    }

    /**
     * Adds the provided address and record to the known sessions
     *
     * @param address the non-null address
     * @param record  the non-null record
     * @return this
     */
    public Keys putSession(@NonNull SessionAddress address, @NonNull Session record) {
        sessions.put(address, record);
        return this;
    }

    /**
     * Adds the provided hash state to the known ones
     *
     * @param device the non-null device
     * @param state  the non-null hash state
     * @return this
     */
    public Keys putState(@NonNull ContactJid device, @NonNull LTHashState state) {
        var oldData = Objects.requireNonNullElseGet(hashStates.get(device), HashMap<PatchType, LTHashState>::new);
        oldData.put(state.name(), state);
        hashStates.put(device, oldData);
        return this;
    }

    /**
     * Adds the provided keys to the app state keys
     *
     * @param jid  the non-null jid of the app key
     * @param keys the keys to add
     * @return this
     */
    public Keys addAppKeys(@NonNull ContactJid jid, @NonNull Collection<AppStateSyncKey> keys) {
        appStateKeys.put(jid, new LinkedList<>(keys));
        return this;
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public AppStateSyncKey getLatestAppKey(@NonNull ContactJid jid) {
        var keys = Objects.requireNonNull(appStateKeys.get(jid), "Missing keys");
        return keys.getLast();
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public LinkedList<AppStateSyncKey> getAppKeys(@NonNull ContactJid jid) {
        return Objects.requireNonNullElseGet(appStateKeys.get(jid), LinkedList::new);
    }

    /**
     * Adds the provided pre key to the pre keys
     *
     * @param preKey the key to add
     * @return this
     */
    public Keys addPreKey(SignalPreKeyPair preKey) {
        preKeys.add(preKey);
        return this;
    }

    /**
     * Returns write counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long writeCounter(boolean increment) {
        return increment ? writeCounter.getAndIncrement() : writeCounter.get();
    }

    /**
     * Returns read counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long readCounter(boolean increment) {
        return increment ? readCounter.getAndIncrement() : readCounter.get();
    }

    /**
     * Returns the id of the last available pre key
     *
     * @return an integer
     */
    public int lastPreKeyId() {
        return preKeys.isEmpty() ? 0 : preKeys.get(preKeys.size() - 1).id();
    }

    @JsonSetter
    private void defaultSignedKey() {
        this.signedKeyPair = SignalSignedKeyPair.of(registrationId, identityKeyPair);
    }

    /**
     * This function sets the companionIdentity field to the value of the companionIdentity parameter,
     * serializes the object, and returns the object.
     *
     * @param companionIdentity The identity of the companion device.
     * @return The object itself.
     */
    public Keys companionIdentity(SignedDeviceIdentity companionIdentity) {
        this.companionIdentity = companionIdentity;
        return this;
    }

    /**
     * Returns the companion identity of this session
     * Only available for web sessions
     *
     * @return an optional
     */
    public Optional<SignedDeviceIdentity> companionIdentity() {
        return Optional.ofNullable(companionIdentity);
    }

    /**
     * Returns all the registered pre keys
     *
     * @return a non-null collection
     */
    public Collection<SignalPreKeyPair> preKeys(){
        return Collections.unmodifiableList(preKeys);
    }

    @Override
    public void dispose() {
        serialize(false);
    }

    @Override
    public void serialize(boolean async) {
        serializer.serializeKeys(this, async);
    }
}