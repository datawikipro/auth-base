package pro.datawiki.auth.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pro.datawiki.auth.base.domain.ReferralRelation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRelationRepository extends JpaRepository<ReferralRelation, Long> {
    Optional<ReferralRelation> findByReferralId(Long referralId);
    List<ReferralRelation> findAllByReferrerId(Long referrerId);
    List<ReferralRelation> findAllByParentReferrerId(Long parentReferrerId);
}
