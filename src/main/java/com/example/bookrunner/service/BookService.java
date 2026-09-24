package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {
    Page<BookResponseDTO> findAll(String keyword, Long categoryId,
                                  BigDecimal minPrice, BigDecimal maxPrice,
                                  int page, int size,
                                  String sortBy, String sortDir);
    void createBook(BookRequestDTO bookRequestDTO);
    void updateBook(Long id, BookRequestDTO bookRequestDTO);
    void deleteBook(List<Long> ids);
}
