package pro.datawiki.auth.base.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.datawiki.auth.base.domain.BalanceTransaction;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.dto.BalanceResponseDto;
import pro.datawiki.auth.base.dto.TransactionResponseDto;
import pro.datawiki.auth.base.repository.BalanceTransactionRepository;
import pro.datawiki.auth.base.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Balance service for credit/debit operations with transaction tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserRepository userRepository;
    private final BalanceTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return new BalanceResponseDto(
                user.getBalance().doubleValue(),
                user.getBalanceCurrency()
        );
    }

    @Transactional
    public TransactionResponseDto credit(Long userId, BigDecimal amount, String description, String referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");

        BigDecimal before = user.getBalance();
        BigDecimal after = before.add(amount);
        user.setBalance(after);
        userRepository.save(user);

        BalanceTransaction tx = buildTx(userId, amount, "credit", description, referenceId, before, after);
        return toDto(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponseDto debit(Long userId, BigDecimal amount, String description, String referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");

        BigDecimal before = user.getBalance();
        if (before.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient balance");

        BigDecimal after = before.subtract(amount);
        user.setBalance(after);
        userRepository.save(user);

        BalanceTransaction tx = buildTx(userId, amount, "debit", description, referenceId, before, after);
        return toDto(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    private BalanceTransaction buildTx(Long userId, BigDecimal amount, String type,
                                       String description, String referenceId,
                                       BigDecimal before, BigDecimal after) {
        BalanceTransaction tx = new BalanceTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setTransactionType(type);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        return tx;
    }

    public TransactionResponseDto toDto(BalanceTransaction tx) {
        return TransactionResponseDto.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .amount(tx.getAmount().doubleValue())
                .type(tx.getTransactionType())
                .description(tx.getDescription())
                .referenceId(tx.getReferenceId())
                .balanceBefore(tx.getBalanceBefore().doubleValue())
                .balanceAfter(tx.getBalanceAfter().doubleValue())
                .createdAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null)
                .build();
    }
}
