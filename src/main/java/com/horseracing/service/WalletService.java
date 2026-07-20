package com.horseracing.service;

import com.horseracing.dto.DepositRequestCreateRequest;
import com.horseracing.dto.DepositRequestRejectRequest;
import com.horseracing.dto.DepositRequestResponse;
import com.horseracing.dto.WalletDepositRequest;
import com.horseracing.dto.WalletResponse;
import com.horseracing.dto.WalletTransactionResponse;
import com.horseracing.entity.DepositRequest;
import com.horseracing.entity.User;
import com.horseracing.entity.Wallet;
import com.horseracing.entity.WalletTransaction;
import com.horseracing.repository.DepositRequestRepository;
import com.horseracing.repository.WalletRepository;
import com.horseracing.repository.WalletTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final CurrentUserService currentUserService;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         DepositRequestRepository depositRequestRepository,
                         CurrentUserService currentUserService) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.currentUserService = currentUserService;
    }

    public WalletResponse getMyWallet(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return toWalletResponse(getOrCreateWallet(user.getUserId()));
    }

    @Transactional
    public DepositRequestResponse deposit(WalletDepositRequest request, HttpServletRequest httpRequest) {
        DepositRequestCreateRequest createRequest = new DepositRequestCreateRequest();
        createRequest.setAmount(request == null ? null : request.getAmount());
        createRequest.setPaymentMethod(request == null ? null : request.getPaymentMethod());
        return createDepositRequest(createRequest, httpRequest);
    }

    @Transactional
    public DepositRequestResponse createDepositRequest(DepositRequestCreateRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        BigDecimal amount = validateAmount(request == null ? null : request.getAmount());
        String paymentMethod = normalizePaymentMethod(request == null ? null : request.getPaymentMethod());
        Wallet wallet = getOrCreateWallet(user.getUserId());

        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setUserId(user.getUserId());
        depositRequest.setWalletId(wallet.getWalletId());
        depositRequest.setAmount(amount);
        depositRequest.setPaymentMethod(paymentMethod);
        depositRequest.setTransferCode(generateTransferCode());
        depositRequest.setQrCodeUrl(buildQrCodeUrl(paymentMethod, amount, depositRequest.getTransferCode()));
        depositRequest.setStatus("Pending");

        return toDepositRequestResponse(depositRequestRepository.save(depositRequest));
    }

    public List<DepositRequestResponse> getMyDepositRequests(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return depositRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toDepositRequestResponse)
                .toList();
    }

    public List<DepositRequestResponse> getAllDepositRequests(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        requireAdmin(user);
        return depositRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDepositRequestResponse)
                .toList();
    }

    @Transactional
    public DepositRequestResponse approveDepositRequest(Integer id, HttpServletRequest request) {
        User admin = currentUserService.getCurrentUser(request);
        requireAdmin(admin);
        DepositRequest depositRequest = getPendingDepositRequest(id);

        Wallet wallet = walletRepository.findById(depositRequest.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet was not found."));
        wallet.setBalance(wallet.getBalance().add(depositRequest.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        depositRequest.setStatus("Approved");
        depositRequest.setApprovedBy(admin.getUserId());
        depositRequest.setApprovedAt(LocalDateTime.now());
        DepositRequest savedRequest = depositRequestRepository.save(depositRequest);

        createTransaction(savedWallet, depositRequest.getAmount(), "Deposit", "Nap tien duoc Admin duyet",
                "DepositRequest", savedRequest.getDepositRequestId());
        return toDepositRequestResponse(savedRequest);
    }

    @Transactional
    public DepositRequestResponse rejectDepositRequest(Integer id, DepositRequestRejectRequest request,
                                                       HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        requireAdmin(admin);
        DepositRequest depositRequest = getPendingDepositRequest(id);
        depositRequest.setStatus("Rejected");
        depositRequest.setAdminNote(request == null ? null : trimToNull(request.getAdminNote()));
        depositRequest.setApprovedBy(admin.getUserId());
        depositRequest.setApprovedAt(LocalDateTime.now());
        return toDepositRequestResponse(depositRequestRepository.save(depositRequest));
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
            throw new IllegalArgumentException("Wallet balance is insufficient for betting.");
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
            throw new IllegalArgumentException("amount must be greater than 0.");
        }
        return amount;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required.");
        }
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"BANK".equals(normalized) && !"MOMO".equals(normalized)) {
            throw new IllegalArgumentException("paymentMethod only accepts BANK or MOMO.");
        }
        return normalized;
    }

    private DepositRequest getPendingDepositRequest(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("depositRequestId is invalid.");
        }
        DepositRequest depositRequest = depositRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deposit request was not found."));
        if (!"Pending".equalsIgnoreCase(depositRequest.getStatus())) {
            throw new IllegalArgumentException("Only Pending deposit requests can be processed.");
        }
        return depositRequest;
    }

    private void requireAdmin(User user) {
        if (!currentUserService.isAdmin(user)) {
            throw new IllegalArgumentException("Only admins can perform this action.");
        }
    }

    private String generateTransferCode() {
        String code;
        do {
            code = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (depositRequestRepository.existsByTransferCode(code));
        return code;
    }

    private String buildQrCodeUrl(String paymentMethod, BigDecimal amount, String transferCode) {
        return "/api/payment-qr?method=" + paymentMethod
                + "&amount=" + amount.toPlainString()
                + "&code=" + transferCode;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private DepositRequestResponse toDepositRequestResponse(DepositRequest request) {
        return new DepositRequestResponse(
                request.getDepositRequestId(),
                request.getUserId(),
                request.getWalletId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getTransferCode(),
                request.getQrCodeUrl(),
                request.getStatus(),
                request.getAdminNote(),
                request.getApprovedBy(),
                request.getApprovedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
