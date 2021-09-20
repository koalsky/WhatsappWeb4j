package it.auties.whatsapp4j.common.protobuf.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionMessage {
  @JsonProperty(value = "2")
  private long timestamp;

  @JsonProperty(value = "1")
  private MessageKey key;
}
