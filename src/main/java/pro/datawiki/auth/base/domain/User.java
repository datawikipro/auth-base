package pro.datawiki.auth.base.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 255)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "balance_currency", length = 3)
    private String balanceCurrency = "RUB";

    @Column(name = "region", length = 50)
    private String region = "RU";

    @Column(name = "virtual_wallet_enabled")
    private boolean virtualWalletEnabled = true;

    @Column(name = "preselected_bookmakers", columnDefinition = "TEXT")
    private String preselectedBookmakers;

    @Column(name = "sport_filters", columnDefinition = "TEXT")
    private String sportFilters;

    @Column(name = "coefficient_types", columnDefinition = "TEXT")
    private String coefficientTypes;

    @Column(name = "custom_subscription_enabled")
    private boolean customSubscriptionEnabled = false;

    @Column(name = "custom_subscription_min_profit", precision = 5, scale = 2)
    private BigDecimal customSubscriptionMinProfit = BigDecimal.ZERO;

    @Column(name = "custom_subscription_sports", columnDefinition = "TEXT")
    private String customSubscriptionSports;

    @Column(name = "custom_subscription_outcomes", columnDefinition = "TEXT")
    private String customSubscriptionOutcomes;

    @Column(name = "custom_subscription_bookmakers", columnDefinition = "TEXT")
    private String customSubscriptionBookmakers;

    @Column(name = "api_key", unique = true, length = 64)
    private String apiKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

    public List<String> getRoleNames() {
        return userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .toList();
    }
}
