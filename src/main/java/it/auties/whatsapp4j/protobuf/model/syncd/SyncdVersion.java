package it.auties.whatsapp4j.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncdVersion {
  @JsonProperty(value = "1")
  private long version;
}
