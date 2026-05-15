package pro.datawiki.auth.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Table permission update request (replaces Python PermissionUpdate).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TablePermissionUpdateRequestDto {

    @JsonProperty("permissions")
    private List<Item> permissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @JsonProperty("tableName")
        private String tableName;

        @JsonProperty("permission")
        private String permission;
    }
}
