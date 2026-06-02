package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.service.SubscriptionService;

import java.util.List;
import java.util.Map;

/**
 * Base subscription controller for /subscriptions/* endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class BaseSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @Value("${internal.api-key:smartbet-internal-secret}")
    private String internalApiKey;

    @PostMapping("/grant")
    public ResponseEntity<SubscriptionResponseDto> grant(@RequestBody GrantSubscriptionRequestDto req) {
        // Caller must be ADMIN (enforced by SecurityConfig)
        java.math.BigDecimal priceVal = req.getPrice() != null ? java.math.BigDecimal.valueOf(req.getPrice()) : null;
        return ResponseEntity.ok(
                subscriptionService.grant(req.getUserId(), req.getFeature(), req.getDurationDays(), priceVal, req.getPriceCurrency()));
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMy() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            List<SubscriptionResponseDto> subs = subscriptionService.getActive(user.getId());
            List<String> features = subs.stream().map(SubscriptionResponseDto::getFeature).toList();
            return ResponseEntity.ok(Map.<String, Object>of(
                    "user_id", user.getId(),
                    "features", features,
                    "subscriptions", subs
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{feature}")
    public ResponseEntity<Map<String, Object>> check(@PathVariable String feature) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            boolean hasAccess = subscriptionService.hasAccess(user.getId(), feature);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "has_access", hasAccess,
                    "feature", feature
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/freeze")
    public ResponseEntity<?> freeze(@RequestBody Map<String, String> req) {
        String feature = req.get("feature");
        if (feature == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing feature parameter"));
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            try {
                SubscriptionResponseDto dto = subscriptionService.freezeSubscription(user.getId(), feature);
                return ResponseEntity.ok(dto);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/unfreeze")
    public ResponseEntity<?> unfreeze(@RequestBody Map<String, String> req) {
        String feature = req.get("feature");
        if (feature == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing feature parameter"));
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            try {
                SubscriptionResponseDto dto = subscriptionService.unfreezeSubscription(user.getId(), feature);
                return ResponseEntity.ok(dto);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Internal bot endpoint: grant subscription using Telegram user_id (internal API key required).
     */
    @PostMapping("/grant-by-telegram")
    public ResponseEntity<SubscriptionResponseDto> grantByTelegram(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestBody GrantSubscriptionRequestDto req) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(403).build();
        }
        java.math.BigDecimal priceVal = req.getPrice() != null ? java.math.BigDecimal.valueOf(req.getPrice()) : null;
        return ResponseEntity.ok(
                subscriptionService.grant(req.getUserId(), req.getFeature(), req.getDurationDays(), priceVal, req.getPriceCurrency()));
    }
}
