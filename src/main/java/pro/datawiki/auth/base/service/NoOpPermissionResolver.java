package pro.datawiki.auth.base.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import pro.datawiki.auth.base.dto.SchemaPermissionDto;

import java.util.List;
import java.util.Map;

/**
 * Default no-op PermissionResolver.
 * Active only when no other implementation is registered (e.g. igaming-auth-microservice).
 */
@Component
@ConditionalOnMissingBean(value = PermissionResolver.class, ignored = NoOpPermissionResolver.class)
public class NoOpPermissionResolver implements PermissionResolver {

    @Override
    public Map<String, String> resolveTablePermissions(Long userId) {
        return Map.of();
    }

    @Override
    public List<SchemaPermissionDto> resolveSchemaPermissions(Long userId) {
        return List.of();
    }

    @Override
    public Map<String, List<String>> resolveHiddenColumns(Long userId) {
        return Map.of();
    }
}
