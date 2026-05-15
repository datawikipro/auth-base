package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.Role;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.*;

import java.util.List;
import java.util.Optional;

/**
 * Role CRUD service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final TablePermissionRepository tablePermissionRepository;
    private final SchemaPermissionRepository schemaPermissionRepository;
    private final ColumnPermissionRepository columnPermissionRepository;

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRole(Long roleId) {
        return roleRepository.findById(roleId).map(this::toDto);
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequestDto req) {
        Role role = new Role();
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setSystem(req.isSystem());
        role.setHasFullAccess(req.isHasFullAccess());
        role.setDefaultSchema(req.getDefaultSchema());
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public Optional<RoleDto> updateRole(Long roleId, UpdateRoleRequestDto req) {
        return roleRepository.findById(roleId).map(role -> {
            if (req.getName() != null) role.setName(req.getName());
            if (req.getDescription() != null) role.setDescription(req.getDescription());
            if (req.getHasFullAccess() != null) role.setHasFullAccess(req.getHasFullAccess());
            if (req.getDefaultSchema() != null) role.setDefaultSchema(req.getDefaultSchema());
            return toDto(roleRepository.save(role));
        });
    }

    @Transactional
    public boolean deleteRole(Long roleId) {
        return roleRepository.findById(roleId).map(role -> {
            if (role.isSystem()) return false;
            roleRepository.delete(role);
            return true;
        }).orElse(false);
    }

    public RoleDto toDto(Role r) {
        return RoleDto.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .isSystem(r.isSystem())
                .hasFullAccess(r.isHasFullAccess())
                .defaultSchema(r.getDefaultSchema())
                .build();
    }
}
