package com.horseracing.controller;

import com.horseracing.dto.WalletDepositRequest;
import com.horseracing.response.ApiResponse;
import com.horseracing.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        return ApiResponse.success(200, "Nap tien vao vi thanh cong", walletService.deposit(body, request));
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
