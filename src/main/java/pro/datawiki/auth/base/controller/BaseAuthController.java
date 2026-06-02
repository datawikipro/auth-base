package pro.datawiki.auth.base.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.domain.Role;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.security.JwtTokenProvider;
import pro.datawiki.auth.base.service.AuthService;
import pro.datawiki.auth.base.service.UserService;
import pro.datawiki.auth.base.service.SubscriptionService;
import pro.datawiki.auth.base.repository.ReferralRelationRepository;
import pro.datawiki.auth.base.repository.PartnerTransactionRepository;
import pro.datawiki.auth.base.domain.ReferralRelation;
import pro.datawiki.auth.base.domain.PartnerTransaction;
import java.time.LocalDateTime;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base authentication controller providing all /auth/* endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SubscriptionService subscriptionService;
    private final ReferralRelationRepository referralRelationRepository;
    private final PartnerTransactionRepository partnerTransactionRepository;

    @Value("${telegram.bot-token:}")
    private String botToken;

    // =========================================================================
    // Health
    // =========================================================================

    @GetMapping({"/", ""})
    public HealthResponseDto health() {
        return HealthResponseDto.builder()
                .status("healthy").database("connected").version("1.0.0").build();
    }

    @GetMapping("/health")
    public HealthResponseDto healthCheck() {
        long count = userService.countUsers();
        return HealthResponseDto.builder()
                .status("healthy").database("connected: " + count + " users").version("1.0.0").build();
    }

    // =========================================================================
    // Login / Logout / Session
    // =========================================================================

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto req, HttpServletResponse response) {
        SessionUserDto session = authService.authenticate(req.getUsername(), req.getPassword());
        if (session == null) {
            return LoginResponseDto.builder().success(false).error("Invalid username or password").build();
        }
        String token = authService.createToken(session);
        setTokenCookie(response, token, authService.getExpireMinutes());
        return LoginResponseDto.builder().success(true).user(session).token(token).authProvider("email").build();
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return Map.of("success", true);
    }

    @GetMapping("/session")
    public SessionResponseDto session(@RequestAttribute(required = false) String currentUsername) {
        // Username is resolved by Spring Security context
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            SessionUserDto session = authService.buildSession(user);
            return SessionResponseDto.builder().success(true).user(session).build();
        }).orElse(SessionResponseDto.builder().success(false).build());
    }

    @PutMapping("/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateUserRequestDto req) {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            // Standard users should NOT be allowed to update their active status, balance or roles!
            req.setIsActive(null);
            req.setBalance(null);
            req.setBalanceCurrency(null);
            req.setRoleIds(null);

            // Allow update of custom subscription fields ONLY if they are PRO (have premium_surebets feature)!
            boolean hasPro = subscriptionService.hasAccess(user.getId(), "premium_surebets");
            if (!hasPro) {
                req.setCustomSubscriptionEnabled(null);
                req.setCustomSubscriptionMinProfit(null);
                req.setCustomSubscriptionSports(null);
                req.setCustomSubscriptionOutcomes(null);
                req.setCustomSubscriptionBookmakers(null);
            }

            userService.updateUser(user.getId(), req);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "message", "Profile updated successfully"
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/verify-token")
    public Map<String, Object> verifyToken(@RequestParam String token) {
        Claims claims = jwtTokenProvider.parseToken(token);
        if (claims == null) {
            return Map.of("valid", false);
        }
        Map<String, Object> result = new HashMap<>(claims);
        result.put("valid", true);
        return result;
    }

    // =========================================================================
    // Registration / Setup
    // =========================================================================

    @GetMapping("/setup-required")
    public Map<String, Boolean> setupRequired() {
        return Map.of("setupRequired", userService.countUsers() == 0);
    }

    @PostMapping("/setup-admin")
    public OperationResponseDto setupAdmin(@RequestBody SetupAdminRequestDto req) {
        if (userService.countUsers() > 0) {
            return OperationResponseDto.fail("Setup already completed");
        }
        try {
            Role adminRole = userService.findOrCreateRole("ADMIN",
                    "Full system administrator with complete access", true, true);
            User user = userService.createUser(req.getUsername(), req.getPassword(), req.getEmail(), req.getFullName());
            userService.assignRoles(user.getId(), List.of(adminRole.getId()));
            return OperationResponseDto.ok();
        } catch (Exception e) {
            log.error("setup-admin error", e);
            return OperationResponseDto.fail(e.getMessage());
        }
    }

    @PostMapping("/register")
    public OperationResponseDto register(@RequestBody RegisterRequestDto req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return OperationResponseDto.fail("Username already exists");
        }
        try {
            Role guestRole = userService.findOrCreateRole("GUEST",
                    "Guest user with read access", true, false);
            User user = userService.createUser(req.getUsername(), req.getPassword(), req.getEmail(), req.getFullName());
            userService.assignRoles(user.getId(), List.of(guestRole.getId()));

            // Handle referral tracking
            if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
                userRepository.findByUsername(req.getReferralCode()).ifPresent(referrer -> {
                    ReferralRelation relation = new ReferralRelation();
                    relation.setReferralId(user.getId());
                    relation.setReferrerId(referrer.getId());
                    
                    // Level 2 relation: check if referrer themselves has a referrer
                    referralRelationRepository.findByReferralId(referrer.getId()).ifPresent(parentRel -> {
                        relation.setParentReferrerId(parentRel.getReferrerId());
                    });
                    
                    referralRelationRepository.save(relation);
                    log.info("Saved referral relation: user={} referred by={} parent={}", 
                            user.getUsername(), referrer.getUsername(), 
                            relation.getParentReferrerId() != null ? relation.getParentReferrerId() : "none");
                });
            }

            return OperationResponseDto.ok();
        } catch (Exception e) {
            log.error("register error", e);
            return OperationResponseDto.fail(e.getMessage());
        }
    }

    // =========================================================================
    // Telegram Auth
    // =========================================================================

    @PostMapping("/telegram")
    public LoginResponseDto telegramAuth(@RequestBody TelegramAuthRequestDto req, HttpServletResponse response) {
        try {
            Long telegramId = req.getTelegramId();
            String tgUsername = "tg_" + telegramId;
            String tgEmail = telegramId + "@datawiki.pro";
            String tgPassword = telegramId + "_salt_secret";
            String firstName = req.getFirstName() != null ? req.getFirstName() : "Telegram";
            String lastName = req.getLastName() != null ? req.getLastName() : "User";
            String fullName = (firstName + " " + lastName).trim();

            Optional<User> linkedUserOpt = userRepository.findByTelegramId(telegramId);
            User user;
            if (linkedUserOpt.isPresent()) {
                user = linkedUserOpt.get();
                log.info("Telegram ID {} is linked to website user: {}", telegramId, user.getUsername());
            } else {
                Optional<User> tgUserOpt = userRepository.findByUsername(tgUsername);
                if (tgUserOpt.isEmpty()) {
                    Role guestRole = userService.findOrCreateRole("GUEST", "Guest user", true, false);
                    user = userService.createUser(tgUsername, tgPassword, tgEmail, fullName);
                    user.setTelegramId(telegramId);
                    userRepository.save(user);
                    userService.assignRoles(user.getId(), List.of(guestRole.getId()));
                    log.info("Created Telegram user: {}", tgUsername);
                } else {
                    user = tgUserOpt.get();
                    if (user.getTelegramId() == null) {
                        user.setTelegramId(telegramId);
                        userRepository.save(user);
                    }
                }
            }

            SessionUserDto session = authService.buildSession(user);
            String token = authService.createTelegramToken(session, telegramId);
            setTokenCookie(response, token, authService.getExpireMinutes());
            return LoginResponseDto.builder().success(true).user(session).token(token).authProvider("telegram").build();
        } catch (Exception e) {
            log.error("Telegram auth error", e);
            return LoginResponseDto.builder().success(false).error(e.getMessage()).build();
        }
    }

    @PostMapping("/telegram-webapp")
    public LoginResponseDto telegramWebappAuth(@RequestBody TelegramWebAppAuthRequestDto req, HttpServletResponse response) {
        try {
            if (!org.springframework.util.StringUtils.hasText(botToken)) {
                return LoginResponseDto.builder().success(false).error("Server configuration error: bot_token missing").build();
            }
            String initData = req.getInitData();
            Map<String, String> params = Arrays.stream(initData.split("&"))
                    .map(p -> p.split("=", 2))
                    .filter(p -> p.length == 2)
                    .collect(Collectors.toMap(p -> p[0], p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)));

            String receivedHash = params.remove("hash");
            if (receivedHash == null) {
                return LoginResponseDto.builder().success(false).error("Missing hash in initData").build();
            }

            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // Calculate secret key: HMAC-SHA256 of botToken with key "WebAppData"
            Mac secretKeyMac = Mac.getInstance("HmacSHA256");
            secretKeyMac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = secretKeyMac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            // Calculate hash: HMAC-SHA256 of dataCheckString with key secretKey
            Mac hashMac = Mac.getInstance("HmacSHA256");
            hashMac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hash = hashMac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String calculated = HexFormat.of().formatHex(hash);

            if (!MessageDigest.isEqual(calculated.getBytes(), receivedHash.getBytes())) {
                return LoginResponseDto.builder().success(false).error("Invalid initData signature").build();
            }

            String userJson = params.get("user");
            if (userJson == null) {
                return LoginResponseDto.builder().success(false).error("User data missing in initData").build();
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> userData = mapper.readValue(userJson, Map.class);
            Long telegramId = ((Number) userData.get("id")).longValue();

            TelegramAuthRequestDto tgReq = new TelegramAuthRequestDto(
                    telegramId,
                    (String) userData.get("username"),
                    (String) userData.get("first_name"),
                    (String) userData.get("last_name")
            );
            return telegramAuth(tgReq, response);
        } catch (Exception e) {
            log.error("Telegram WebApp auth error", e);
            return LoginResponseDto.builder().success(false).error(e.getMessage()).build();
        }
    }

    @GetMapping("/telegram/{telegramId}")
    public ResponseEntity<Map<String, Object>> getUserByTelegramId(@PathVariable Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(u -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true, "id", u.getId(), "username", u.getUsername())))
                .or(() -> userRepository.findByUsername("tg_" + telegramId)
                        .map(u -> ResponseEntity.ok(Map.<String, Object>of(
                                "success", true, "id", u.getId(), "username", u.getUsername()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/link-telegram")
    public ResponseEntity<Map<String, Object>> linkTelegram(@RequestBody TelegramAuthRequestDto req) {
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        User currentUser = currentUserOpt.get();
        Long telegramId = req.getTelegramId();

        if (telegramId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing telegramId"));
        }

        // Check if this telegramId is already linked to another user
        Optional<User> alreadyLinkedOpt = userRepository.findByTelegramId(telegramId);
        if (alreadyLinkedOpt.isPresent() && !alreadyLinkedOpt.get().getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Telegram account is already linked to another user"));
        }

        // Merge data from temporary tg_{telegramId} user if it exists
        String tempUsername = "tg_" + telegramId;
        Optional<User> tempUserOpt = userRepository.findByUsername(tempUsername);
        if (tempUserOpt.isPresent() && !tempUserOpt.get().getId().equals(currentUser.getId())) {
            User tempUser = tempUserOpt.get();

            // Merge balance
            if (tempUser.getBalance() != null && tempUser.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                if (currentUser.getBalance() == null) {
                    currentUser.setBalance(tempUser.getBalance());
                } else {
                    currentUser.setBalance(currentUser.getBalance().add(tempUser.getBalance()));
                }
            }

            // Merge virtual wallet settings / copy subscriptions if necessary
            if (tempUser.isCustomSubscriptionEnabled()) {
                currentUser.setCustomSubscriptionEnabled(true);
                currentUser.setCustomSubscriptionMinProfit(tempUser.getCustomSubscriptionMinProfit());
                currentUser.setCustomSubscriptionSports(tempUser.getCustomSubscriptionSports());
                currentUser.setCustomSubscriptionOutcomes(tempUser.getCustomSubscriptionOutcomes());
                currentUser.setCustomSubscriptionBookmakers(tempUser.getCustomSubscriptionBookmakers());
            }

            // Delete temp user so we don't have a duplicated account
            userRepository.delete(tempUser);
            log.info("Merged temporary Telegram user {} into standard user {}", tempUsername, currentUsername);
        }

        currentUser.setTelegramId(telegramId);
        userRepository.save(currentUser);

        return ResponseEntity.ok(Map.of("success", true, "message", "Telegram account linked successfully"));
    }

    @GetMapping("/referral/stats")
    public ResponseEntity<Map<String, Object>> getReferralStats() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            List<ReferralRelation> l1Relations = referralRelationRepository.findAllByReferrerId(user.getId());
            List<ReferralRelation> l2Relations = referralRelationRepository.findAllByParentReferrerId(user.getId());
            
            // Get referred users details
            List<Map<String, Object>> recentRegistrations = new ArrayList<>();
            for (ReferralRelation r : l1Relations) {
                userRepository.findById(r.getReferralId()).ifPresent(u -> {
                    recentRegistrations.add(Map.of(
                            "username", u.getUsername(),
                            "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : LocalDateTime.now().toString(),
                            "level", 1
                    ));
                });
            }
            for (ReferralRelation r : l2Relations) {
                userRepository.findById(r.getReferralId()).ifPresent(u -> {
                    recentRegistrations.add(Map.of(
                            "username", u.getUsername(),
                            "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : LocalDateTime.now().toString(),
                            "level", 2
                    ));
                });
            }
            
            // Sort recent registrations by date descending
            recentRegistrations.sort((a, b) -> ((String) b.get("createdAt")).compareTo((String) a.get("createdAt")));
            
            List<PartnerTransaction> txs = partnerTransactionRepository.findAllByPartnerId(user.getId());
            List<Map<String, Object>> recentTransactions = new ArrayList<>();
            BigDecimal totalL1Earnings = BigDecimal.ZERO;
            BigDecimal totalL2Earnings = BigDecimal.ZERO;
            
            for (PartnerTransaction t : txs) {
                String buyerName = userRepository.findById(t.getBuyerId()).map(User::getUsername).orElse("unknown");
                recentTransactions.add(Map.of(
                        "buyerUsername", buyerName,
                        "paymentAmount", t.getPaymentAmount(),
                        "commissionAmount", t.getCommissionAmount(),
                        "currency", t.getCurrency(),
                        "level", t.getLevel(),
                        "createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : LocalDateTime.now().toString()
                ));
                if (t.getLevel() == 1) {
                    totalL1Earnings = totalL1Earnings.add(t.getCommissionAmount());
                } else if (t.getLevel() == 2) {
                    totalL2Earnings = totalL2Earnings.add(t.getCommissionAmount());
                }
            }
            
            // Sort recent transactions by date descending
            recentTransactions.sort((a, b) -> ((String) b.get("createdAt")).compareTo((String) a.get("createdAt")));
            
            return ResponseEntity.ok(Map.<String, Object>of(
                    "referralLink", "https://smartbet.guru/?ref=" + user.getUsername(),
                    "level1Count", l1Relations.size(),
                    "level2Count", l2Relations.size(),
                    "level1Earnings", totalL1Earnings,
                    "level2Earnings", totalL2Earnings,
                    "totalEarnings", totalL1Earnings.add(totalL2Earnings),
                    "recentRegistrations", recentRegistrations.stream().limit(10).toList(),
                    "recentTransactions", recentTransactions.stream().limit(10).toList()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setTokenCookie(HttpServletResponse response, String token, long expireMinutes) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (expireMinutes * 60));
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
