package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.datawiki.auth.base.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);

    @Query("SELECT COUNT(u) FROM User u")
    long countAll();
}
