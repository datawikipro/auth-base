package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Schema-level permission entry in session user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaPermissionDto {

    @JsonProperty("schemaName")
    private String schemaName;

    @JsonProperty("permission")
    private String permission;

    @JsonProperty("scope")
    private String scope;
}
