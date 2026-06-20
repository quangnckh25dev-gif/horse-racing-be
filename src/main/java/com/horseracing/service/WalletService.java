package com.horseracing.service;

import com.horseracing.dto.WalletDepositRequest;
import com.horseracing.dto.WalletResponse;
import com.horseracing.dto.WalletTransactionResponse;
import com.horseracing.entity.User;
import com.horseracing.entity.Wallet;
import com.horseracing.entity.WalletTransaction;
import com.horseracing.repository.WalletRepository;
import com.horseracing.repository.WalletTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CurrentUserService currentUserService;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         CurrentUserService currentUserService) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.currentUserService = currentUserService;
    }

    public WalletResponse getMyWallet(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return toWalletResponse(getOrCreateWallet(user.getUserId()));
    }

    @Transactional
    public WalletResponse deposit(WalletDepositRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        BigDecimal amount = validateAmount(request == null ? null : request.getAmount());
        Wallet wallet = getOrCreateWallet(user.getUserId());
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, amount, "Deposit", "Nap tien vao vi", "Wallet", saved.getWalletId());
        return toWalletResponse(saved);
    }

    public List<WalletTransactionResponse> getMyTransactions(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        Wallet wallet = getOrCreateWallet(user.getUserId());
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public Wallet debitForBet(Integer userId, BigDecimal amount, Integer betId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(validAmount) < 0) {
            throw new IllegalArgumentException("So du vi khong du de dat cuoc");
        }

        wallet.setBalance(wallet.getBalance().subtract(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount.negate(), "BetPlaced", "Dat cuoc race", "Bet", betId);
        return saved;
    }

    @Transactional
    public Wallet creditBetWin(Integer userId, BigDecimal amount, Integer betId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount, "BetWon", "Nhan tien thang cuoc", "Bet", betId);
        return saved;
    }

    public Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount phai lon hon 0");
        }
        return amount;
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String description,
                                   String relatedEntity, Integer relatedEntityId) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWalletId(wallet.getWalletId());
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setDescription(description);
        transaction.setRelatedEntity(relatedEntity);
        transaction.setRelatedEntityId(relatedEntityId);
        walletTransactionRepository.save(transaction);
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getTransactionId(),
                transaction.getWalletId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getDescription(),
                transaction.getRelatedEntity(),
                transaction.getRelatedEntityId(),
                transaction.getCreatedAt()
        );
    }
}
