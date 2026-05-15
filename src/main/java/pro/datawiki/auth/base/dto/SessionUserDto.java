package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Full user session returned after successful authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionUserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("permissions")
    private Map<String, String> permissions;

    @JsonProperty("schemaPermissions")
    private List<SchemaPermissionDto> schemaPermissions;

    @JsonProperty("hiddenColumns")
    private Map<String, List<String>> hiddenColumns;

    @JsonProperty("defaultSchema")
    private String defaultSchema;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    @JsonProperty("hasFullAccess")
    private boolean hasFullAccess;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("authProvider")
    private String authProvider;

    @JsonProperty("externalId")
    private String externalId;
}
