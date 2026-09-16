package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookService {
    Page<BookResponseDTO> findAll();
    void createBook(BookRequestDTO bookRequestDTO);
    void updateBook(Long id, BookRequestDTO bookRequestDTO);
    void deleteBook(List<Long> ids);
}
