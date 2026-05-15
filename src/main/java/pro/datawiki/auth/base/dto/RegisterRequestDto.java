package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Registration request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;
}
