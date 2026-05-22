package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.Role;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.domain.UserRole;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.RoleRepository;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.repository.UserRoleRepository;

import java.util.List;
import java.util.Optional;

/**
 * User CRUD and role assignment service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.countAll();
    }

    @Transactional
    public User createUser(String username, String password, String email, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User createUser(CreateUserRequestDto req) {
        return createUser(req.getUsername(), req.getPassword(), req.getEmail(), req.getFullName());
    }

    @Transactional
    public Optional<User> updateUser(Long userId, UpdateUserRequestDto req) {
        return userRepository.findById(userId).map(user -> {
            if (req.getEmail() != null) user.setEmail(req.getEmail());
            if (req.getFullName() != null) user.setFullName(req.getFullName());
            if (req.getIsActive() != null) user.setActive(req.getIsActive());
            if (req.getPassword() != null) user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            if (req.getBalance() != null) user.setBalance(req.getBalance());
            if (req.getBalanceCurrency() != null) user.setBalanceCurrency(req.getBalanceCurrency());
            if (req.getRegion() != null) user.setRegion(req.getRegion());
            if (req.getVirtualWalletEnabled() != null) user.setVirtualWalletEnabled(req.getVirtualWalletEnabled());
            if (req.getPreselectedBookmakers() != null) user.setPreselectedBookmakers(req.getPreselectedBookmakers());
            if (req.getSportFilters() != null) user.setSportFilters(req.getSportFilters());
            if (req.getCoefficientTypes() != null) user.setCoefficientTypes(req.getCoefficientTypes());
            if (req.getCustomSubscriptionEnabled() != null) user.setCustomSubscriptionEnabled(req.getCustomSubscriptionEnabled());
            if (req.getCustomSubscriptionMinProfit() != null) user.setCustomSubscriptionMinProfit(req.getCustomSubscriptionMinProfit());
            if (req.getCustomSubscriptionSports() != null) user.setCustomSubscriptionSports(req.getCustomSubscriptionSports());
            if (req.getCustomSubscriptionOutcomes() != null) user.setCustomSubscriptionOutcomes(req.getCustomSubscriptionOutcomes());
            if (req.getCustomSubscriptionBookmakers() != null) user.setCustomSubscriptionBookmakers(req.getCustomSubscriptionBookmakers());
            return userRepository.save(user);
        });
    }

    @Transactional
    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) return false;
        userRepository.deleteById(userId);
        return true;
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        List<UserRole> newRoles = roleIds.stream().map(roleId -> {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).toList();
        userRoleRepository.saveAll(newRoles);
    }

    /**
     * Find or create a role by name. Used during setup-admin and register.
     */
    @Transactional
    public Role findOrCreateRole(String name, String description, boolean system, boolean hasFullAccess) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            r.setSystem(system);
            r.setHasFullAccess(hasFullAccess);
            return roleRepository.save(r);
        });
    }

    public UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .isActive(u.isActive())
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null)
                .updatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : null)
                .roles(u.getRoleNames())
                .balance(u.getBalance())
                .balanceCurrency(u.getBalanceCurrency())
                .region(u.getRegion())
                .virtualWalletEnabled(u.isVirtualWalletEnabled())
                .preselectedBookmakers(u.getPreselectedBookmakers())
                .sportFilters(u.getSportFilters())
                .coefficientTypes(u.getCoefficientTypes())
                .customSubscriptionEnabled(u.isCustomSubscriptionEnabled())
                .customSubscriptionMinProfit(u.getCustomSubscriptionMinProfit())
                .customSubscriptionSports(u.getCustomSubscriptionSports())
                .customSubscriptionOutcomes(u.getCustomSubscriptionOutcomes())
                .customSubscriptionBookmakers(u.getCustomSubscriptionBookmakers())
                .build();
    }
}
