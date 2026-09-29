package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    private final BookService bookService;

    @PatchMapping("/{id}/featured")
    public ResponseEntity<ApiResponse<BookResponseDTO>> toggleFeatured(@PathVariable Long id) {
        BookResponseDTO book = bookService.toggleFeatured(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái nổi bật thành công", book));
    }
}
