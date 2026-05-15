package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.dto.*;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.service.BalanceService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Base balance controller for /balance/* endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class BaseBalanceController {

    private final BalanceService balanceService;
    private final UserRepository userRepository;

    // ---- By user_id ----

    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(balanceService.getBalance(userId));
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<TransactionResponseDto> credit(@PathVariable Long userId,
                                                         @RequestBody TransactionRequestDto req) {
        return ResponseEntity.ok(balanceService.credit(
                userId, BigDecimal.valueOf(req.getAmount()), req.getDescription(), req.getReferenceId()));
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<TransactionResponseDto> debit(@PathVariable Long userId,
                                                        @RequestBody TransactionRequestDto req) {
        return ResponseEntity.ok(balanceService.debit(
                userId, BigDecimal.valueOf(req.getAmount()), req.getDescription(), req.getReferenceId()));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionResponseDto>> getTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(balanceService.getTransactions(userId));
    }

    // ---- By Telegram ID ----

    @GetMapping("/telegram/{telegramId}")
    public ResponseEntity<BalanceResponseDto> getBalanceByTelegram(@PathVariable Long telegramId) {
        String username = "tg_" + telegramId;
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(balanceService.getBalance(u.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/telegram/{telegramId}/credit")
    public ResponseEntity<TransactionResponseDto> creditByTelegram(@PathVariable Long telegramId,
                                                                    @RequestBody TransactionRequestDto req) {
        String username = "tg_" + telegramId;
        return userRepository.findByUsername(username).map(u ->
                ResponseEntity.ok(balanceService.credit(
                        u.getId(), BigDecimal.valueOf(req.getAmount()), req.getDescription(), req.getReferenceId()))
        ).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/telegram/{telegramId}/debit")
    public ResponseEntity<TransactionResponseDto> debitByTelegram(@PathVariable Long telegramId,
                                                                   @RequestBody TransactionRequestDto req) {
        String username = "tg_" + telegramId;
        return userRepository.findByUsername(username).map(u ->
                ResponseEntity.ok(balanceService.debit(
                        u.getId(), BigDecimal.valueOf(req.getAmount()), req.getDescription(), req.getReferenceId()))
        ).orElse(ResponseEntity.notFound().build());
    }
}
