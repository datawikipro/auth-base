package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Balance response for /balance/{userId} and /balance/telegram/{id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceResponseDto {

    @JsonProperty("balance")
    private double balance;

    @JsonProperty("currency")
    private String currency;
}
