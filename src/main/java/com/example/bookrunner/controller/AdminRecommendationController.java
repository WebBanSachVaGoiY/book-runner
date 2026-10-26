package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.service.RecSysAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Trigger huấn luyện lại mô hình RecSys và xem đánh giá từ trang quản trị.
 * Bảo vệ bởi ROLE_ADMIN (đã phủ bởi matcher /api/v1/admin/** trong SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/recommendations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecommendationController {

    private final RecSysAdminService recSysAdminService;

    @PostMapping("/train")
    public ResponseEntity<ApiResponse<Map<String, Object>>> train(
            @RequestParam(defaultValue = "all") String type) {
        Map<String, Object> result = recSysAdminService.train(type);
        return ResponseEntity.ok(ApiResponse.success(
                "Huấn luyện lại RecSys thành công (type=" + type + ")", result));
    }

    @GetMapping("/evaluate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluate(
            @RequestParam(defaultValue = "10") int topK) {
        Map<String, Object> result = recSysAdminService.evaluate(topK);
        return ResponseEntity.ok(ApiResponse.success(
                "Đo lường đánh giá chất lượng mô hình RecSys thành công", result));
    }
}
