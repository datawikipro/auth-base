package pro.datawiki.auth.base.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter @Setter @NoArgsConstructor
public class UserSubscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature", nullable = false, length = 50)
    private String feature;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "frozen", nullable = false)
    private boolean frozen = false;

    @Column(name = "freeze_started_at")
    private LocalDateTime freezeStartedAt;

    @Column(name = "remaining_seconds")
    private Long remainingSeconds;

    @Column(name = "freeze_count")
    private Integer freezeCount = 0;

    @Column(name = "accumulated_freeze_days")
    private Integer accumulatedFreezeDays = 0;

    @Column(name = "last_freeze_year")
    private Integer lastFreezeYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
