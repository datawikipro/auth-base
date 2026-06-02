package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pro.datawiki.auth.base.domain.PartnerTransaction;

import java.util.List;

@Repository
public interface PartnerTransactionRepository extends JpaRepository<PartnerTransaction, Long> {
    List<PartnerTransaction> findAllByPartnerId(Long partnerId);
}
