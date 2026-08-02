package com.horseracing.service;

import com.horseracing.dto.DepositRequestCreateRequest;
import com.horseracing.dto.DepositRequestRejectRequest;
import com.horseracing.dto.DepositRequestResponse;
import com.horseracing.dto.WalletDepositRequest;
import com.horseracing.dto.WalletResponse;
import com.horseracing.dto.WalletTransactionResponse;
import com.horseracing.dto.WalletTransactionSummaryResponse;
import com.horseracing.dto.WithdrawalRequestCreateRequest;
import com.horseracing.dto.WithdrawalRejectRequest;
import com.horseracing.dto.WithdrawalRequestResponse;
import com.horseracing.entity.DepositRequest;
import com.horseracing.entity.User;
import com.horseracing.entity.Wallet;
import com.horseracing.entity.WalletTransaction;
import com.horseracing.entity.WithdrawalRequest;
import com.horseracing.repository.DepositRequestRepository;
import com.horseracing.repository.UserRepository;
import com.horseracing.repository.WalletRepository;
import com.horseracing.repository.WalletTransactionRepository;
import com.horseracing.repository.WithdrawalRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WalletService {

    // Rut tien: so tien toi thieu moi lan
    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10000");

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         DepositRequestRepository depositRequestRepository,
                         WithdrawalRequestRepository withdrawalRequestRepository,
                         UserRepository userRepository,
                         CurrentUserService currentUserService) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    // ========================= RÚT TIỀN (Withdrawal) =========================

    @Transactional
    public WithdrawalRequestResponse createWithdrawalRequest(WithdrawalRequestCreateRequest request,
                                                             HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);   // moi role deu rut duoc
        if (request == null) {
            throw new IllegalArgumentException("Withdrawal data is required.");
        }
        BigDecimal amount = validateAmount(request.getAmount());
        if (amount.compareTo(MIN_WITHDRAWAL) < 0) {
            throw new IllegalArgumentException("Minimum withdrawal amount is 10,000 VND.");
        }
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        String accNumber = trimToNull(request.getBankAccountNumber());
        String accName = trimToNull(request.getBankAccountName());
        if (accNumber == null) {
            throw new IllegalArgumentException("Bank account number is required.");
        }
        if (accName == null) {
            throw new IllegalArgumentException("Bank account name is required.");
        }

        Wallet wallet = getOrCreateWallet(user.getUserId());
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for this withdrawal.");
        }

        // Tru (giu) tien ngay khi tao yeu cau
        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setUserId(user.getUserId());
        wr.setWalletId(savedWallet.getWalletId());
        wr.setAmount(amount);
        wr.setPaymentMethod(paymentMethod);
        wr.setBankName(trimToNull(request.getBankName()));
        wr.setBankAccountNumber(accNumber);
        wr.setBankAccountName(accName);
        wr.setStatus("Pending");
        WithdrawalRequest saved = withdrawalRequestRepository.save(wr);

        createTransaction(savedWallet, amount.negate(), "Withdrawal", "Withdrawal requested",
                "WithdrawalRequest", saved.getWithdrawalRequestId());
        return toWithdrawalRequestResponse(saved);
    }

    public List<WithdrawalRequestResponse> getMyWithdrawalRequests(HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream().map(this::toWithdrawalRequestResponse).toList();
    }

    public List<WithdrawalRequestResponse> getAllWithdrawalRequests(HttpServletRequest httpRequest) {
        requireAdmin(currentUserService.getCurrentUser(httpRequest));
        return withdrawalRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toWithdrawalRequestResponse).toList();
    }

    @Transactional
    public WithdrawalRequestResponse approveWithdrawalRequest(Integer id, HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        requireAdmin(admin);
        WithdrawalRequest wr = getPendingWithdrawalRequest(id);
        // Tien da tru luc tao -> approve chi danh dau da chuyen
        wr.setStatus("Approved");
        wr.setApprovedBy(admin.getUserId());
        wr.setApprovedAt(LocalDateTime.now());
        return toWithdrawalRequestResponse(withdrawalRequestRepository.save(wr));
    }

    @Transactional
    public WithdrawalRequestResponse rejectWithdrawalRequest(Integer id, WithdrawalRejectRequest request,
                                                             HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        requireAdmin(admin);
        WithdrawalRequest wr = getPendingWithdrawalRequest(id);

        // Hoan tien lai vi (idempotent: chi hoan khi chuyen tu Pending)
        Wallet wallet = walletRepository.findById(wr.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet was not found."));
        wallet.setBalance(wallet.getBalance().add(wr.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);
        createTransaction(savedWallet, wr.getAmount(), "WithdrawalRefund", "Withdrawal rejected - refunded",
                "WithdrawalRequest", wr.getWithdrawalRequestId());

        wr.setStatus("Rejected");
        wr.setAdminNote(request == null ? null : trimToNull(request.getAdminNote()));
        wr.setApprovedBy(admin.getUserId());
        wr.setApprovedAt(LocalDateTime.now());
        return toWithdrawalRequestResponse(withdrawalRequestRepository.save(wr));
    }

    private WithdrawalRequest getPendingWithdrawalRequest(Integer id) {
        WithdrawalRequest wr = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request was not found."));
        if (!"Pending".equalsIgnoreCase(wr.getStatus())) {
            throw new IllegalArgumentException("Only pending withdrawal requests can be processed.");
        }
        return wr;
    }

    private WithdrawalRequestResponse toWithdrawalRequestResponse(WithdrawalRequest wr) {
        User user = userRepository.findById(wr.getUserId()).orElse(null);
        return new WithdrawalRequestResponse(
                wr.getWithdrawalRequestId(),
                wr.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
                wr.getWalletId(),
                wr.getAmount(),
                wr.getPaymentMethod(),
                wr.getBankName(),
                wr.getBankAccountNumber(),
                wr.getBankAccountName(),
                wr.getStatus(),
                wr.getAdminNote(),
                wr.getApprovedBy(),
                wr.getApprovedAt(),
                wr.getCreatedAt(),
                wr.getUpdatedAt()
        );
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
        // Moi role deu co vi -> ai cung nap tien duoc (khong gioi han Spectator)
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
        return getAllDepositRequests(request, null, null, null, null);
    }

    public List<DepositRequestResponse> getAllDepositRequests(HttpServletRequest request, String status,
                                                              String paymentMethod, String date,
                                                              String keyword) {
        User user = currentUserService.getCurrentUser(request);
        requireAdmin(user);
        String cleanStatus = normalizeOptionalStatus(status);
        String cleanPaymentMethod = paymentMethod == null || paymentMethod.isBlank()
                ? null
                : normalizePaymentMethod(paymentMethod);
        LocalDate cleanDate = parseDate(date);
        String cleanKeyword = trimToNull(keyword);

        return depositRequestRepository.searchAdminDepositRequests(cleanStatus, cleanPaymentMethod, cleanDate, cleanKeyword)
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

        createTransaction(savedWallet, depositRequest.getAmount(), "Deposit", "Deposit approved by Admin",
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
        return getMyTransactions(request, null, null);
    }

    public List<WalletTransactionResponse> getMyTransactions(HttpServletRequest request, String filter,
                                                             String transactionType) {
        User user = currentUserService.getCurrentUser(request);
        Wallet wallet = getOrCreateWallet(user.getUserId());
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId())
                .stream()
                .filter(transaction -> matchesTransactionFilter(transaction, filter))
                .filter(transaction -> matchesTransactionType(transaction, transactionType))
                .map(this::toTransactionResponse)
                .toList();
    }

    public WalletTransactionSummaryResponse getMyTransactionSummary(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        Wallet wallet = getOrCreateWallet(user.getUserId());
        List<WalletTransaction> transactions =
                walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId());

        BigDecimal totalMoneyIn = BigDecimal.ZERO;
        BigDecimal totalMoneyOut = BigDecimal.ZERO;
        for (WalletTransaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                totalMoneyIn = totalMoneyIn.add(amount);
            } else {
                totalMoneyOut = totalMoneyOut.add(amount.abs());
            }
        }

        return new WalletTransactionSummaryResponse(
                wallet.getBalance(),
                totalMoneyIn,
                totalMoneyOut,
                transactions.size()
        );
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
        createTransaction(saved, validAmount.negate(), "BetPlaced", "Race bet placed", "Bet", betId);
        return saved;
    }

    @Transactional
    public Wallet debitForBetTicket(Integer userId, BigDecimal amount, Integer ticketId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(validAmount) < 0) {
            throw new IllegalArgumentException("Wallet balance is insufficient for betting.");
        }

        wallet.setBalance(wallet.getBalance().subtract(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount.negate(), "BetPlaced", "Parlay ticket placed", "BetTicket", ticketId);
        return saved;
    }

    @Transactional
    public Wallet creditBetWin(Integer userId, BigDecimal amount, Integer betId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount, "BetWon", "Bet payout received", "Bet", betId);
        return saved;
    }

    @Transactional
    public Wallet creditBetTicketWin(Integer userId, BigDecimal amount, Integer ticketId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount, "BetWon", "Parlay ticket payout received", "BetTicket", ticketId);
        return saved;
    }

    @Transactional
    public Wallet creditPrizeAward(Integer ownerUserId, BigDecimal amount, Integer raceId, Integer resultId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(ownerUserId);
        boolean alreadyAwardedByResult = walletTransactionRepository
                .existsByWalletIdAndTransactionTypeAndRelatedEntityAndRelatedEntityId(
                        wallet.getWalletId(), "PrizeAwarded", "RaceResult", resultId);
        boolean alreadyAwardedByRace = walletTransactionRepository
                .existsByWalletIdAndTransactionTypeAndRelatedEntityAndRelatedEntityId(
                        wallet.getWalletId(), "PrizeAwarded", "Race", raceId);
        if (alreadyAwardedByResult || alreadyAwardedByRace) {
            return wallet;
        }

        wallet.setBalance(wallet.getBalance().add(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount, "PrizeAwarded", "Race placement prize awarded",
                "RaceResult", resultId);
        return saved;
    }

    @Transactional
    public Wallet creditDepositComplaint(Integer userId, BigDecimal amount, Integer complaintId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet wallet = getOrCreateWallet(userId);
        boolean alreadyCredited = walletTransactionRepository
                .existsByWalletIdAndTransactionTypeAndRelatedEntityAndRelatedEntityId(
                        wallet.getWalletId(), "Deposit", "DepositComplaint", complaintId);
        if (alreadyCredited) {
            return wallet;
        }

        wallet.setBalance(wallet.getBalance().add(validAmount));
        Wallet saved = walletRepository.save(wallet);
        createTransaction(saved, validAmount, "Deposit", "Deposit complaint resolved by Admin",
                "DepositComplaint", complaintId);
        return saved;
    }

    @Transactional
    public void transferJockeyDeal(Integer ownerUserId, Integer jockeyUserId, BigDecimal amount, Integer invitationId) {
        BigDecimal validAmount = validateAmount(amount);
        Wallet ownerWallet = getOrCreateWallet(ownerUserId);
        if (ownerWallet.getBalance().compareTo(validAmount) < 0) {
            throw new IllegalArgumentException("Owner wallet balance is insufficient for this jockey deal.");
        }

        Wallet jockeyWallet = getOrCreateWallet(jockeyUserId);
        ownerWallet.setBalance(ownerWallet.getBalance().subtract(validAmount));
        jockeyWallet.setBalance(jockeyWallet.getBalance().add(validAmount));

        Wallet savedOwnerWallet = walletRepository.save(ownerWallet);
        Wallet savedJockeyWallet = walletRepository.save(jockeyWallet);
        createTransaction(savedOwnerWallet, validAmount.negate(), "JockeyDealPaid",
                "Jockey deal paid", "JockeyInvitation", invitationId);
        createTransaction(savedJockeyWallet, validAmount, "JockeyDealReceived",
                "Jockey deal received", "JockeyInvitation", invitationId);
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

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void autoRejectExpiredDepositRequests() {
        LocalDateTime now = LocalDateTime.now();
        depositRequestRepository.autoRejectExpiredPendingRequests(now.minusSeconds(30), now);
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

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String cleanStatus = status.trim();
        if (!"Pending".equalsIgnoreCase(cleanStatus)
                && !"Approved".equalsIgnoreCase(cleanStatus)
                && !"Rejected".equalsIgnoreCase(cleanStatus)) {
            throw new IllegalArgumentException("status only accepts Pending, Approved, or Rejected.");
        }
        return cleanStatus.substring(0, 1).toUpperCase(Locale.ROOT)
                + cleanStatus.substring(1).toLowerCase(Locale.ROOT);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("date must be in yyyy-MM-dd format.");
        }
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

    private void requireRole(User user, String roleName, String message) {
        if (user == null || user.getRole() == null || !roleName.equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new IllegalArgumentException(message);
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

    private boolean matchesTransactionFilter(WalletTransaction transaction, String filter) {
        String normalized = normalizeFilterValue(filter);
        if (normalized == null || "all".equals(normalized)) {
            return true;
        }

        BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
        if ("moneyin".equals(normalized) || "in".equals(normalized)) {
            return amount.compareTo(BigDecimal.ZERO) >= 0;
        }
        if ("moneyout".equals(normalized) || "out".equals(normalized)) {
            return amount.compareTo(BigDecimal.ZERO) < 0;
        }

        String transactionKey = normalizeFilterValue(transaction.getTransactionType());
        String transactionLabel = normalizeFilterValue(toTransactionTypeLabel(transaction.getTransactionType()));
        return normalized.equals(transactionKey) || normalized.equals(transactionLabel);
    }

    private boolean matchesTransactionType(WalletTransaction transaction, String transactionType) {
        String normalized = normalizeFilterValue(transactionType);
        if (normalized == null || "all".equals(normalized)) {
            return true;
        }
        return normalized.equals(normalizeFilterValue(transaction.getTransactionType()));
    }

    private String normalizeFilterValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
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

    private String toTransactionTypeLabel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "Deposit" -> "Deposit";
            case "BetPlaced" -> "Bet Placed";
            case "BetWon" -> "Bet Won";
            case "BetRefund" -> "Refunded";
            case "PrizeAwarded" -> "Prize Awarded";
            case "Withdrawal" -> "Withdrawal";
            case "WithdrawalRefund" -> "Withdrawal Refund";
            default -> value;
        };
    }

    private DepositRequestResponse toDepositRequestResponse(DepositRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);
        return new DepositRequestResponse(
                request.getDepositRequestId(),
                request.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
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
