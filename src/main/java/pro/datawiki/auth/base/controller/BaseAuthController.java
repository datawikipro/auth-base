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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

            if (!userRepository.existsByUsername(tgUsername)) {
                Role guestRole = userService.findOrCreateRole("GUEST", "Guest user", true, false);
                User user = userService.createUser(tgUsername, tgPassword, tgEmail, fullName);
                userService.assignRoles(user.getId(), List.of(guestRole.getId()));
                log.info("Created Telegram user: {}", tgUsername);
            }

            SessionUserDto session = authService.authenticate(tgUsername, tgPassword);
            if (session == null) {
                return LoginResponseDto.builder().success(false).error("Authentication failed after creation").build();
            }

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
        String tgUsername = "tg_" + telegramId;
        return userRepository.findByUsername(tgUsername)
                .map(u -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true, "id", u.getId(), "username", u.getUsername())))
                .orElse(ResponseEntity.notFound().build());
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
