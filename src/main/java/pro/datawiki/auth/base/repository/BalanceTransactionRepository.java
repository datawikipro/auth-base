package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.datawiki.auth.base.domain.BalanceTransaction;

import java.util.List;

public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, Integer> {
    List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
