package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Login response returned to client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("user")
    private SessionUserDto user;

    @JsonProperty("token")
    private String token;

    @JsonProperty("error")
    private String error;

    @JsonProperty("authProvider")
    private String authProvider;
}
