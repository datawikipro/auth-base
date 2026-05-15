package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Subscription response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("starts_at")
    private String startsAt;

    @JsonProperty("expires_at")
    private String expiresAt;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_at")
    private String createdAt;
}
