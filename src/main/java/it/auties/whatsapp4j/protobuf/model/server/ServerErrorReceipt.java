package it.auties.whatsapp4j.protobuf.model.server;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ServerErrorReceipt {
  @JsonProperty(value = "1")
  private String stanzaId;
}
