package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.UserSubscription;
import pro.datawiki.auth.base.dto.SubscriptionResponseDto;
import pro.datawiki.auth.base.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    public SubscriptionResponseDto toDto(UserSubscription s) {
        return SubscriptionResponseDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .feature(s.getFeature())
                .startsAt(s.getStartsAt() != null ? s.getStartsAt().toString() : null)
                .expiresAt(s.getExpiresAt() != null ? s.getExpiresAt().toString() : null)
                .isActive(s.isActive())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }
}
