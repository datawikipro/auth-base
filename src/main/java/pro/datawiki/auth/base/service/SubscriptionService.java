package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.UserSubscription;
import pro.datawiki.auth.base.dto.SubscriptionResponseDto;
import pro.datawiki.auth.base.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.repository.ReferralRelationRepository;
import pro.datawiki.auth.base.repository.PartnerTransactionRepository;
import pro.datawiki.auth.base.domain.ReferralRelation;
import pro.datawiki.auth.base.domain.PartnerTransaction;
import pro.datawiki.auth.base.domain.User;

/**
 * Subscription management service.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResponseDto grant(Long userId, String feature, Integer durationDays) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = durationDays != null ? now.plusDays(durationDays) : null;

        // Upsert: update existing or create new
        UserSubscription sub = subscriptionRepository
                .findByUserIdAndFeature(userId, feature)
                .orElseGet(UserSubscription::new);

        sub.setUserId(userId);
        sub.setFeature(feature);
        sub.setStartsAt(now);
        sub.setExpiresAt(expiresAt);
        sub.setActive(true);

        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> getActive(Long userId) {
        return subscriptionRepository.findActiveByUserId(userId, LocalDateTime.now())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, String feature) {
        return subscriptionRepository.hasActiveSubscription(userId, feature, LocalDateTime.now());
    }

    @Transactional
    public SubscriptionResponseDto freezeSubscription(Long userId, String feature) {
        UserSubscription sub = subscriptionRepository.findByUserIdAndFeature(userId, feature)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (sub.isFrozen()) {
            throw new IllegalStateException("Subscription is already frozen");
        }
        if (!sub.isActive() || sub.getExpiresAt() == null || sub.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Subscription is not active or has already expired");
        }

        // Check/reset yearly limit
        int currentYear = LocalDate.now().getYear();
        if (sub.getLastFreezeYear() == null || sub.getLastFreezeYear() != currentYear) {
            sub.setLastFreezeYear(currentYear);
            sub.setFreezeCount(0);
            sub.setAccumulatedFreezeDays(0);
        }

        if (sub.getFreezeCount() >= 3) {
            throw new IllegalStateException("Maximum of 3 freezes per year exceeded");
        }
        if (sub.getAccumulatedFreezeDays() >= 30) {
            throw new IllegalStateException("Maximum of 30 total frozen days per year exceeded");
        }

        // Calculate remaining seconds
        long remaining = Duration.between(LocalDateTime.now(), sub.getExpiresAt()).getSeconds();
        if (remaining <= 0) {
            throw new IllegalStateException("Subscription is already expired");
        }

        sub.setFrozen(true);
        sub.setFreezeStartedAt(LocalDateTime.now());
        sub.setRemainingSeconds(remaining);
        sub.setExpiresAt(null); // Prevent expiration checks while frozen
        sub.setFreezeCount(sub.getFreezeCount() + 1);

        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionResponseDto unfreezeSubscription(Long userId, String feature) {
        UserSubscription sub = subscriptionRepository.findByUserIdAndFeature(userId, feature)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!sub.isFrozen()) {
            throw new IllegalStateException("Subscription is not frozen");
        }

        // Calculate freeze duration to accumulate
        long freezeDurationSeconds = Duration.between(sub.getFreezeStartedAt(), LocalDateTime.now()).getSeconds();
        int freezeDays = (int) Math.ceil(freezeDurationSeconds / 86400.0);
        
        sub.setAccumulatedFreezeDays(sub.getAccumulatedFreezeDays() + freezeDays);
        sub.setExpiresAt(LocalDateTime.now().plusSeconds(sub.getRemainingSeconds()));
        sub.setFrozen(false);
        sub.setFreezeStartedAt(null);
        sub.setRemainingSeconds(null);

        return toDto(subscriptionRepository.save(sub));
    }

    public SubscriptionResponseDto toDto(UserSubscription s) {
        return SubscriptionResponseDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .feature(s.getFeature())
                .startsAt(s.getStartsAt() != null ? s.getStartsAt().toString() : null)
                .expiresAt(s.getExpiresAt() != null ? s.getExpiresAt().toString() : null)
                .isActive(s.isActive() && !s.isFrozen())
                .isFrozen(s.isFrozen())
                .freezeCount(s.getFreezeCount())
                .accumulatedFreezeDays(s.getAccumulatedFreezeDays())
                .remainingSeconds(s.getRemainingSeconds())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }
}
