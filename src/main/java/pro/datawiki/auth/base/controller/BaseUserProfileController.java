package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.dto.UpdateUserRequestDto;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.service.UserService;
import pro.datawiki.auth.base.service.SubscriptionService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseUserProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

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

    @PostMapping("/generate-api-key")
    public ResponseEntity<Map<String, Object>> generateApiKey() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            String newKey = java.util.UUID.randomUUID().toString().replace("-", "") +
                            java.util.UUID.randomUUID().toString().replace("-", "");
            user.setApiKey(newKey);
            userRepository.save(user);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "apiKey", newKey));
        }).orElse(ResponseEntity.notFound().build());
    }
}
