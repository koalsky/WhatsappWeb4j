package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Pushname {
  @JsonProperty(value = "2")
  private String pushname;

  @JsonProperty(value = "1")
  private String id;
}
