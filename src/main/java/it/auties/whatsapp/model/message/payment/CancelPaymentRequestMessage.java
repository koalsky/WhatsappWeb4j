package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;

/**
 * A model class that represents a message that cancels a {@link RequestPaymentMessage}.
 */
@ProtobufMessageName("Message.CancelPaymentRequestMessage")
public record CancelPaymentRequestMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
                ChatMessageKey key
) implements PaymentMessage {
    @Override
    public MessageType type() {
        return MessageType.CANCEL_PAYMENT_REQUEST;
    }
}
