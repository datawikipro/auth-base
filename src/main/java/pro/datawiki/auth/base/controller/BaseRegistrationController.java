package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.domain.Role;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.domain.ReferralRelation;
import pro.datawiki.auth.base.dto.OperationResponseDto;
import pro.datawiki.auth.base.dto.RegisterRequestDto;
import pro.datawiki.auth.base.dto.SetupAdminRequestDto;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.repository.ReferralRelationRepository;
import pro.datawiki.auth.base.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseRegistrationController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ReferralRelationRepository referralRelationRepository;

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
}
