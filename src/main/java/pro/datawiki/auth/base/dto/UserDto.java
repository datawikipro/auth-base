package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.math.BigDecimal;

/**
 * User data transfer object for admin list/detail responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("balanceCurrency")
    private String balanceCurrency;

    @JsonProperty("region")
    private String region;

    @JsonProperty("virtualWalletEnabled")
    private boolean virtualWalletEnabled;

    @JsonProperty("preselectedBookmakers")
    private String preselectedBookmakers;

    @JsonProperty("sportFilters")
    private String sportFilters;

    @JsonProperty("coefficientTypes")
    private String coefficientTypes;

    @JsonProperty("customSubscriptionEnabled")
    private boolean customSubscriptionEnabled;

    @JsonProperty("customSubscriptionMinProfit")
    private BigDecimal customSubscriptionMinProfit;

    @JsonProperty("customSubscriptionSports")
    private String customSubscriptionSports;

    @JsonProperty("customSubscriptionOutcomes")
    private String customSubscriptionOutcomes;

    @JsonProperty("customSubscriptionBookmakers")
    private String customSubscriptionBookmakers;
}
