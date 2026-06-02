package pro.datawiki.auth.base.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartnerTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId; // Partner receiving commission

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId; // User who bought subscription

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "level", nullable = false)
    private Integer level; // 1 or 2

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
