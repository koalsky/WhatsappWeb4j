package it.auties.whatsapp4j.protobuf.model.client;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class DNSSource {
  @JsonProperty(value = "16")
  private boolean appCached;

  @JsonProperty(value = "15")
  private DNSSourceDNSResolutionMethod dnsMethod;

  @Accessors(fluent = true)
  public enum DNSSourceDNSResolutionMethod {
    SYSTEM(0),
    GOOGLE(1),
    HARDCODED(2),
    OVERRIDE(3),
    FALLBACK(4);

    private final @Getter int index;

    DNSSourceDNSResolutionMethod(int index) {
      this.index = index;
    }

    @JsonCreator
    public static DNSSourceDNSResolutionMethod forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
