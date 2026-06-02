package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Grant subscription request (admin).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrantSubscriptionRequestDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("duration_days")
    private Integer durationDays;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("priceCurrency")
    private String priceCurrency;
}
