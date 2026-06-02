package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.domain.Role;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.service.AuthService;
import pro.datawiki.auth.base.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseTelegramAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${telegram.bot-token:}")
    private String botToken;

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

    private void setTokenCookie(HttpServletResponse response, String token, long expireMinutes) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (expireMinutes * 60));
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
