package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Column permission update request (hidden columns per role).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnPermissionUpdateRequestDto {

    @JsonProperty("permissions")
    private List<Item> permissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @JsonProperty("tableName")
        private String tableName;

        @JsonProperty("columnName")
        private String columnName;

        @JsonProperty("permission")
        private String permission;
    }
}
