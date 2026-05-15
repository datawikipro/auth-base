package pro.datawiki.auth.base.service;

import pro.datawiki.auth.base.dto.SchemaPermissionDto;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for resolving user permissions into the session.
 * <p>
 * auth-base knows nothing about table/schema/column permissions — those live in
 * consuming modules (e.g. table-manager-auth-microservice). Each module provides
 * its own bean implementing this interface. The default no-op implementation is
 * used when no bean is supplied (e.g. igaming-auth-microservice with no tables).
 */
public interface PermissionResolver {

    /**
     * Highest-wins table permission map: tableName → permission level.
     */
    Map<String, String> resolveTablePermissions(Long userId);

    /**
     * List of schema-level permissions for the user.
     */
    List<SchemaPermissionDto> resolveSchemaPermissions(Long userId);

    /**
     * Hidden columns per table: tableName → list of hidden column names.
     */
    Map<String, List<String>> resolveHiddenColumns(Long userId);
}
