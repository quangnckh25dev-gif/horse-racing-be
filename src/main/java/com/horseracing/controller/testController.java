package com.horseracing.controller;

import com.horseracing.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testController {

    @GetMapping("/api/test")
    public ApiResponse<String> test() {
        return ApiResponse.<String>builder()
                .status(200)
                .message("API chạy thành công")
                .data("Hello Horse Racing")
                .build();
    }
}