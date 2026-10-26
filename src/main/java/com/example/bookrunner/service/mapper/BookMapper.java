package com.example.bookrunner.service.mapper;

import com.example.bookrunner.dto.CategoryDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.model.Book;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper dùng chung để chuyển {@link Book} → {@link BookResponseDTO}.
 * Tách ra khỏi BookServiceImpl để tầng gợi ý tái sử dụng, tránh lặp logic.
 */
@Component
@RequiredArgsConstructor
public class BookMapper {

    private final ModelMapper mapper;

    public BookResponseDTO toDTO(Book book) {
        if (book == null) {
            return null;
        }
        BookResponseDTO dto = mapper.map(book, BookResponseDTO.class);
        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getId());
            dto.setCategoryName(book.getCategory().getName());
            dto.setCategory(mapper.map(book.getCategory(), CategoryDTO.class));
        }
        return dto;
    }

    public List<BookResponseDTO> toDTOList(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }
        return books.stream().map(this::toDTO).toList();
    }
}
