package it.auties.whatsapp4j.beta.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.auties.whatsapp4j.beta.utils.MultiDeviceCypher;
import it.auties.whatsapp4j.beta.utils.SignedKeyPair;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.serialization.KeyPairDeserializer;
import it.auties.whatsapp4j.common.serialization.KeyPairSerializer;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.security.KeyPair;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true, chain = true)
public class MultiDeviceKeysManager extends WhatsappKeysManager {
    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private @NonNull KeyPair ephemeralKeyPair;

    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private @NonNull KeyPair signedIdentityKey;

    @JsonProperty
    private @NonNull SignedKeyPair signedPreKey;

    @JsonProperty
    private @NonNull int registrationId;

    public MultiDeviceKeysManager(){
        super(Base64.getEncoder().encodeToString(BinaryArray.random(16).data()), CypherUtils.randomKeyPair(), null, null, null, null);
        this.ephemeralKeyPair = CypherUtils.randomKeyPair();
        this.signedIdentityKey = CypherUtils.randomKeyPair();
        this.signedPreKey = MultiDeviceCypher.randomSignedPreKey();
        this.registrationId = ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, Short.MAX_VALUE) & 0x3fff;
    }
}