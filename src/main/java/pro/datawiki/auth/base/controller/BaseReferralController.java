package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.domain.ReferralRelation;
import pro.datawiki.auth.base.domain.PartnerTransaction;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.repository.ReferralRelationRepository;
import pro.datawiki.auth.base.repository.PartnerTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseReferralController {

    private final UserRepository userRepository;
    private final ReferralRelationRepository referralRelationRepository;
    private final PartnerTransactionRepository partnerTransactionRepository;

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
}
