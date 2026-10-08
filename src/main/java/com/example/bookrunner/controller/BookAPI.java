package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookAPI {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookResponseDTO>>> getAllBook(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "isFeatured", required = false) Boolean isFeatured,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "16") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        Page<BookResponseDTO> books = bookService.findAll(keyword, categoryId, minPrice, maxPrice, isFeatured,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(books));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponseDTO>> getBookById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookService.findById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addBook(@Valid @RequestBody BookRequestDTO bookRequestDTO) {
        bookService.createBook(bookRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Thêm sách mới thành công", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateBook(@PathVariable Long id,
            @Valid @RequestBody BookRequestDTO bookRequestDTO) {
        bookService.updateBook(id, bookRequestDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteBook(@RequestBody List<Long> ids) {
        bookService.deleteBook(ids);
        return ResponseEntity.ok(ApiResponse.success("Ngừng kinh doanh sách thành công", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBook(@RequestBody List<Long> ids) {
        bookService.restoreBook(ids);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục kinh doanh sách thành công", null));
    }
}
