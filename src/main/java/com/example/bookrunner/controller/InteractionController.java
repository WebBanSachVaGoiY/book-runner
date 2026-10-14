package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.InteractionLogRequest;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<Void>> logInteraction(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody InteractionLogRequest request) {

        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.success("Bỏ qua ghi nhận tương tác cho khách vãng lai.", null));
        }

        // Kích hoạt ghi nhận bất đồng bộ (Non-blocking worker pool)
        interactionService.logInteractionAsync(currentUser.getId(), request);

        return ResponseEntity.ok(ApiResponse.success("Đã tiếp nhận ghi nhận tương tác.", null));
    }
}
