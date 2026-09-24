package com.example.bookrunner.controller;

import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookAPI {

    private final BookService bookService;

    @GetMapping
    public Page<BookResponseDTO> getAllBook(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return bookService.findAll(keyword, categoryId, minPrice, maxPrice,
                page, size, sortBy, sortDir);
    }

    @PostMapping
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO bookRequestDTO) {
        bookService.createBook(bookRequestDTO);
        return ResponseEntity.status(201).body("Thêm sách mới thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBook(@PathVariable Long id, @RequestBody BookRequestDTO bookRequestDTO) {
        bookService.updateBook(id, bookRequestDTO);
        return ResponseEntity.status(200).body("Cập nhật thông tin thành công");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteBook(@RequestBody List<Long> ids) {
        bookService.deleteBook(ids);
        return ResponseEntity.status(200).body("Xóa sách thành công");
    }
}
