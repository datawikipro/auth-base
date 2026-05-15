package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Generic operation result (replaces Python's UserOperationResponse).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationResponseDto {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error")
    private String error;

    public static OperationResponseDto ok() {
        return new OperationResponseDto(true, null);
    }

    public static OperationResponseDto fail(String error) {
        return new OperationResponseDto(false, error);
    }
}
