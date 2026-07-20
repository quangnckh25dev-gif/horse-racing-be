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
        return ApiResponse.success(200, "Lay vi cua toi thanh cong", walletService.getMyWallet(request));
    }

    @PostMapping("/api/wallets/deposit")
    public ApiResponse<?> deposit(@RequestBody WalletDepositRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Tao yeu cau nap tien thanh cong", walletService.deposit(body, request));
    }

    @PostMapping("/api/wallets/deposit-requests")
    public ApiResponse<?> createDepositRequest(@RequestBody DepositRequestCreateRequest body, HttpServletRequest request) {
        return ApiResponse.success(201, "Tao yeu cau nap tien thanh cong", walletService.createDepositRequest(body, request));
    }

    @GetMapping("/api/wallets/deposit-requests/mine")
    public ApiResponse<?> getMyDepositRequests(HttpServletRequest request) {
        return ApiResponse.success(200, "Lay danh sach yeu cau nap tien cua toi thanh cong", walletService.getMyDepositRequests(request));
    }

    @GetMapping("/api/admin/deposit-requests")
    public ApiResponse<?> getAllDepositRequests(HttpServletRequest request) {
        return ApiResponse.success(200, "Lay danh sach yeu cau nap tien thanh cong", walletService.getAllDepositRequests(request));
    }

    @PutMapping("/api/admin/deposit-requests/{id}/approve")
    public ApiResponse<?> approveDepositRequest(@PathVariable Integer id, HttpServletRequest request) {
        return ApiResponse.success(200, "Duyet yeu cau nap tien thanh cong", walletService.approveDepositRequest(id, request));
    }

    @PutMapping("/api/admin/deposit-requests/{id}/reject")
    public ApiResponse<?> rejectDepositRequest(@PathVariable Integer id, @RequestBody DepositRequestRejectRequest body,
                                               HttpServletRequest request) {
        return ApiResponse.success(200, "Tu choi yeu cau nap tien thanh cong", walletService.rejectDepositRequest(id, body, request));
    }

    @GetMapping("/api/wallets/transactions")
    public ApiResponse<?> getMyTransactions(HttpServletRequest request) {
        return ApiResponse.success(200, "Lay lich su giao dich vi thanh cong", walletService.getMyTransactions(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }
}
