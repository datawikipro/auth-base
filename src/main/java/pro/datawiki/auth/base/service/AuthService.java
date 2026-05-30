package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.security.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Core authentication service: authenticate, build session, create JWT.
 * Permission resolution is delegated to {@link PermissionResolver} —
 * each consuming module provides its own implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionResolver permissionResolver;

    @Transactional(readOnly = true)
    public SessionUserDto authenticate(String username, String password) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return null;
        User user = opt.get();
        if (!user.isActive()) return null;
        if (!passwordEncoder.matches(password, user.getPasswordHash())) return null;
        return buildSession(user);
    }

    @Transactional(readOnly = true)
    public SessionUserDto buildSession(User user) {
        boolean hasFullAccess = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().isHasFullAccess());

        String defaultSchema = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getDefaultSchema())
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        // Delegate permission resolution to the module-specific implementation
        Map<String, String> permMap = permissionResolver.resolveTablePermissions(user.getId());
        var schemaPerms = permissionResolver.resolveSchemaPermissions(user.getId());
        var hiddenColumns = permissionResolver.resolveHiddenColumns(user.getId());

        return SessionUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName() != null ? user.getFullName() : "")
                .roles(user.getRoleNames())
                .permissions(permMap)
                .schemaPermissions(schemaPerms)
                .hiddenColumns(hiddenColumns)
                .defaultSchema(defaultSchema)
                .isAdmin(hasFullAccess)
                .hasFullAccess(hasFullAccess)
                .isActive(user.isActive())
                .authProvider("email")
                .telegramId(user.getTelegramId())
                .balance(user.getBalance())
                .balanceCurrency(user.getBalanceCurrency())
                .region(user.getRegion())
                .virtualWalletEnabled(user.isVirtualWalletEnabled())
                .preselectedBookmakers(user.getPreselectedBookmakers())
                .sportFilters(user.getSportFilters())
                .coefficientTypes(user.getCoefficientTypes())
                .customSubscriptionEnabled(user.isCustomSubscriptionEnabled())
                .customSubscriptionMinProfit(user.getCustomSubscriptionMinProfit())
                .customSubscriptionSports(user.getCustomSubscriptionSports())
                .customSubscriptionOutcomes(user.getCustomSubscriptionOutcomes())
                .customSubscriptionBookmakers(user.getCustomSubscriptionBookmakers())
                .build();
    }

    public String createToken(SessionUserDto session) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", session.getId());
        claims.put("email", session.getEmail());
        claims.put("fullName", session.getFullName());
        claims.put("role", session.isAdmin() ? "ADMIN" : "STUDENT");
        return jwtTokenProvider.createToken(claims, session.getUsername());
    }

    public String createTelegramToken(SessionUserDto session, Long telegramId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", session.getId());
        claims.put("email", session.getEmail());
        claims.put("fullName", session.getFullName());
        claims.put("telegram_id", telegramId);
        claims.put("auth_provider", "telegram");
        claims.put("role", session.isAdmin() ? "ADMIN" : "GUEST");
        return jwtTokenProvider.createToken(claims, session.getUsername());
    }

    public long getExpireMinutes() {
        return jwtTokenProvider.getExpireMinutes();
    }
}
