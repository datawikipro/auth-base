package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Login request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;
}
