package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Schema permission update request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaPermissionUpdateRequestDto {

    @JsonProperty("permissions")
    private List<Item> permissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @JsonProperty("schemaName")
        private String schemaName;

        @JsonProperty("permission")
        private String permission;

        @JsonProperty("scope")
        private String scope;
    }
}
