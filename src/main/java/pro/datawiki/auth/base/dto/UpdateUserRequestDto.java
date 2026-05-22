package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.math.BigDecimal;

/**
 * Request body for updating an existing user (admin).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserRequestDto {

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("password")
    private String password;

    @JsonProperty("roleIds")
    private List<Long> roleIds;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("balanceCurrency")
    private String balanceCurrency;

    @JsonProperty("region")
    private String region;

    @JsonProperty("virtualWalletEnabled")
    private Boolean virtualWalletEnabled;

    @JsonProperty("preselectedBookmakers")
    private String preselectedBookmakers;

    @JsonProperty("sportFilters")
    private String sportFilters;

    @JsonProperty("coefficientTypes")
    private String coefficientTypes;

    @JsonProperty("customSubscriptionEnabled")
    private Boolean customSubscriptionEnabled;

    @JsonProperty("customSubscriptionMinProfit")
    private BigDecimal customSubscriptionMinProfit;

    @JsonProperty("customSubscriptionSports")
    private String customSubscriptionSports;

    @JsonProperty("customSubscriptionOutcomes")
    private String customSubscriptionOutcomes;

    @JsonProperty("customSubscriptionBookmakers")
    private String customSubscriptionBookmakers;
}
