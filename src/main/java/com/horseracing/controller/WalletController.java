package com.horseracing.controller;

import com.horseracing.dto.DepositRequestCreateRequest;
import com.horseracing.dto.DepositRequestRejectRequest;
import com.horseracing.dto.WalletDepositRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/api/wallets/me")
    public ApiResponse<?> getMyWallet(HttpServletRequest request) {
        return ApiResponse.success(200, "My wallet loaded successfully", walletService.getMyWallet(request));
    }

    @PostMapping("/api/wallets/deposit")
    public ApiResponse<?> deposit(@RequestBody WalletDepositRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Deposit request created successfully", walletService.deposit(body, request));
    }

    @PostMapping("/api/wallets/deposit-requests")
    public ApiResponse<?> createDepositRequest(@RequestBody DepositRequestCreateRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Deposit request created successfully", walletService.createDepositRequest(body, request));
    }

    @GetMapping("/api/wallets/deposit-requests/mine")
    public ApiResponse<?> getMyDepositRequests(HttpServletRequest request) {
        return ApiResponse.success(200, "My deposit requests loaded successfully", walletService.getMyDepositRequests(request));
    }

    @GetMapping("/api/admin/deposit-requests")
    public ApiResponse<?> getAllDepositRequests(HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit requests loaded successfully", walletService.getAllDepositRequests(request));
    }

    @PutMapping("/api/admin/deposit-requests/{id}/approve")
    public ApiResponse<?> approveDepositRequest(@PathVariable Integer id, HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit request approved successfully", walletService.approveDepositRequest(id, request));
    }

    @PutMapping("/api/admin/deposit-requests/{id}/reject")
    public ApiResponse<?> rejectDepositRequest(@PathVariable Integer id, @RequestBody DepositRequestRejectRequest body,
                                               HttpServletRequest request) {
        return ApiResponse.success(200, "Deposit request rejected successfully", walletService.rejectDepositRequest(id, body, request));
    }

    @GetMapping("/api/wallets/transactions")
    public ApiResponse<?> getMyTransactions(HttpServletRequest request) {
        return ApiResponse.success(200, "Wallet transaction history loaded successfully", walletService.getMyTransactions(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
