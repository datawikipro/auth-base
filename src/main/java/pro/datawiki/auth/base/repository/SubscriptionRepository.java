package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.datawiki.auth.base.domain.UserSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @Query("SELECT s FROM UserSubscription s WHERE s.userId = :userId AND s.active = true AND s.frozen = false AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<UserSubscription> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) > 0 FROM UserSubscription s WHERE s.userId = :userId AND s.feature = :feature AND s.active = true AND s.frozen = false AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    boolean hasActiveSubscription(@Param("userId") Long userId, @Param("feature") String feature, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSubscription s WHERE s.userId = :userId AND s.feature = :feature")
    Optional<UserSubscription> findByUserIdAndFeature(@Param("userId") Long userId, @Param("feature") String feature);
}
