package com.horseracing.service;

import com.horseracing.dto.DepositComplaintRequest;
import com.horseracing.dto.DepositComplaintResponse;
import com.horseracing.entity.DepositComplaint;
import com.horseracing.entity.DepositRequest;
import com.horseracing.entity.User;
import com.horseracing.repository.DepositComplaintRepository;
import com.horseracing.repository.DepositRequestRepository;
import com.horseracing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DepositComplaintService {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_RESOLVED = "Resolved";
    private static final String STATUS_REJECTED = "Rejected";

    private final DepositComplaintRepository depositComplaintRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public DepositComplaintService(DepositComplaintRepository depositComplaintRepository,
                                   DepositRequestRepository depositRequestRepository,
                                   UserRepository userRepository,
                                   CurrentUserService currentUserService) {
        this.depositComplaintRepository = depositComplaintRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public DepositComplaintResponse createComplaint(DepositComplaintRequest request, HttpServletRequest httpRequest) {
        User user = currentUserService.getCurrentUser(httpRequest);
        requireRole(user, "Spectator");
        if (request == null) {
            throw new IllegalArgumentException("Complaint data is required.");
        }

        DepositRequest depositRequest = findLinkedDepositRequest(request, user.getUserId());
        BigDecimal amount = request.getAmount() != null
                ? validateAmount(request.getAmount())
                : depositRequest == null ? null : depositRequest.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("amount is required.");
        }

        String paymentMethod = request.getPaymentMethod() != null
                ? normalizePaymentMethod(request.getPaymentMethod())
                : depositRequest == null ? null : depositRequest.getPaymentMethod();
        if (paymentMethod == null) {
            throw new IllegalArgumentException("paymentMethod is required.");
        }

        String reason = trimToNull(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("reason is required.");
        }

        DepositComplaint complaint = new DepositComplaint();
        complaint.setUserId(user.getUserId());
        complaint.setDepositRequestId(depositRequest == null ? request.getDepositRequestId() : depositRequest.getDepositRequestId());
        complaint.setTransferCode(depositRequest == null ? trimToNull(request.getTransferCode()) : depositRequest.getTransferCode());
        complaint.setAmount(amount);
        complaint.setPaymentMethod(paymentMethod);
        complaint.setReason(reason);
        complaint.setEvidenceUrl(trimToNull(request.getEvidenceUrl()));
        complaint.setStatus(STATUS_PENDING);
        return toResponse(depositComplaintRepository.save(complaint));
    }

    public List<DepositComplaintResponse> getMyComplaints(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser(request);
        return depositComplaintRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DepositComplaintResponse> getAllComplaints(HttpServletRequest request) {
        User admin = currentUserService.getCurrentUser(request);
        requireAdmin(admin);
        return depositComplaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepositComplaintResponse resolveComplaint(Integer id, DepositComplaintRequest request,
                                                     HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        requireAdmin(admin);
        DepositComplaint complaint = getPendingComplaint(id);
        complaint.setStatus(STATUS_RESOLVED);
        complaint.setAdminNote(request == null ? null : trimToNull(request.getAdminNote()));
        complaint.setResolvedBy(admin.getUserId());
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(depositComplaintRepository.save(complaint));
    }

    @Transactional
    public DepositComplaintResponse rejectComplaint(Integer id, DepositComplaintRequest request,
                                                    HttpServletRequest httpRequest) {
        User admin = currentUserService.getCurrentUser(httpRequest);
        requireAdmin(admin);
        String adminNote = request == null ? null : trimToNull(request.getAdminNote());
        if (adminNote == null) {
            throw new IllegalArgumentException("adminNote is required when rejecting a complaint.");
        }
        DepositComplaint complaint = getPendingComplaint(id);
        complaint.setStatus(STATUS_REJECTED);
        complaint.setAdminNote(adminNote);
        complaint.setResolvedBy(admin.getUserId());
        complaint.setResolvedAt(LocalDateTime.now());
        return toResponse(depositComplaintRepository.save(complaint));
    }

    private DepositComplaint getPendingComplaint(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("complaintId is invalid.");
        }
        DepositComplaint complaint = depositComplaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deposit complaint was not found."));
        if (!STATUS_PENDING.equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalArgumentException("Only Pending complaints can be processed.");
        }
        return complaint;
    }

    private DepositRequest findLinkedDepositRequest(DepositComplaintRequest request, Integer userId) {
        DepositRequest depositRequest = null;
        if (request.getDepositRequestId() != null) {
            depositRequest = depositRequestRepository.findById(request.getDepositRequestId())
                    .orElseThrow(() -> new IllegalArgumentException("Deposit request was not found."));
        } else if (trimToNull(request.getTransferCode()) != null) {
            depositRequest = depositRequestRepository.findByTransferCode(request.getTransferCode().trim())
                    .orElse(null);
        }
        if (depositRequest != null && !depositRequest.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Deposit request does not belong to the current user.");
        }
        return depositRequest;
    }

    private void requireAdmin(User user) {
        if (!currentUserService.isAdmin(user)) {
            throw new IllegalArgumentException("Only admins can perform this action.");
        }
    }

    private void requireRole(User user, String roleName) {
        if (user == null || user.getRole() == null || !roleName.equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new IllegalArgumentException("Only spectators can create deposit complaints.");
        }
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DepositComplaintResponse toResponse(DepositComplaint complaint) {
        User user = userRepository.findById(complaint.getUserId()).orElse(null);
        return new DepositComplaintResponse(
                complaint.getComplaintId(),
                complaint.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                complaint.getDepositRequestId(),
                complaint.getTransferCode(),
                complaint.getAmount(),
                complaint.getPaymentMethod(),
                complaint.getReason(),
                complaint.getEvidenceUrl(),
                complaint.getStatus(),
                complaint.getAdminNote(),
                complaint.getResolvedBy(),
                complaint.getResolvedAt(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt()
        );
    }
}
