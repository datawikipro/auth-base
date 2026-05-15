package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Telegram Mini App authentication request (initData HMAC validation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramWebAppAuthRequestDto {

    @JsonProperty("initData")
    private String initData;
}
