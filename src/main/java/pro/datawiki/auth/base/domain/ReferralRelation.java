package pro.datawiki.auth.base.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_relations", uniqueConstraints = {
        @UniqueConstraint(columnNames = "referral_id")
})
@Getter @Setter @NoArgsConstructor
public class ReferralRelation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referral_id", nullable = false)
    private Long referralId; // Invited user

    @Column(name = "referrer_id", nullable = false)
    private Long referrerId; // Inviter (Level 1)

    @Column(name = "parent_referrer_id")
    private Long parentReferrerId; // Inviter's inviter (Level 2)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
