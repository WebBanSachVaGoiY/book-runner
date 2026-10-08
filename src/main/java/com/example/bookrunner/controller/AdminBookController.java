package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookResponseDTO>>> getAllBooks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "16") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        Page<BookResponseDTO> books = bookService.findAllForAdmin(keyword, categoryId, minPrice, maxPrice,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(books));
    }

    @PatchMapping("/{id}/featured")
    public ResponseEntity<ApiResponse<BookResponseDTO>> toggleFeatured(@PathVariable("id") Long id) {
        BookResponseDTO book = bookService.toggleFeatured(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái nổi bật thành công", book));
    }
}
