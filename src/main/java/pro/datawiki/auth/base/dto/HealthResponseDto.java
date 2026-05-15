package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Health check response (/ and /health endpoints).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponseDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("database")
    private String database;

    @JsonProperty("version")
    private String version;
}
