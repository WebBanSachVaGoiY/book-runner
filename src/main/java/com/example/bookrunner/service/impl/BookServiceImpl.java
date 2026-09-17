package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.exception.DuplicateUniqueFieldException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CategoryRepository;
import com.example.bookrunner.service.BookService;
import com.example.bookrunner.util.ListToPageUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper mapper;

    // Lấy tất cả các sách
    @Override
    public Page<BookResponseDTO> findAll() {
        List<Book> result = bookRepository.findAll();
        List<BookResponseDTO> bookResponseDTOList = result.stream()
                .map(response -> mapper.map(result, BookResponseDTO.class)).collect(Collectors.toList());
        Pageable pageable = PageRequest.of(0, 16);
        return ListToPageUtil.convertListToPage(bookResponseDTOList, pageable);
    }

    // Thêm sách
    @Override
    public void createBook(BookRequestDTO bookRequestDTO) {
        Book newBook = mapper.map(bookRequestDTO, Book.class);

        // Set default values cho các cột NOT NULL (ModelMapper không áp dụng
        // @Builder.Default)
        if (newBook.getStockQuantity() == null)
            newBook.setStockQuantity(0);
        if (newBook.getActive() == null)
            newBook.setActive(true);
        if (newBook.getIsFeatured() == null)
            newBook.setIsFeatured(false);
        if (newBook.getAverageRating() == null)
            newBook.setAverageRating(0.0);
        if (newBook.getTotalReviews() == null)
            newBook.setTotalReviews(0);
        if (newBook.getLanguage() == null)
            newBook.setLanguage("Tiếng Việt");
        if (bookRepository.findByIsbn(newBook.getIsbn()).isPresent())
            throw new DuplicateUniqueFieldException("Mã định danh bị trùng!");

        // Lookup Category từ DB thay vì dùng transient object
        if (bookRequestDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookRequestDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                            "Category không tồn tại với id: " + bookRequestDTO.getCategoryId()));
            newBook.setCategory(category);
        }

        bookRepository.save(newBook);
    }

    // Cap nhat thong tin sach
    @Override
    public void updateBook(Long id, BookRequestDTO bookRequestDTO) {
        Optional<Book> updating = bookRepository.findById(id);
        if (updating.isEmpty())
            throw new ItemNotFoundException("Sách không tồn tại!");
        else {
            Book existingBook = updating.get();
            // Chỉ cập nhật các field được gửi lên, giữ lại id
            mapper.map(bookRequestDTO, existingBook);
            if (bookRepository.findByIsbn(existingBook.getIsbn()).isPresent())
                throw new DuplicateUniqueFieldException("Mã định danh bị trùng!");
            if (bookRequestDTO.getCategoryId() != null) {
                Category category = categoryRepository.findById(bookRequestDTO.getCategoryId())
                        .orElseThrow(() -> new ItemNotFoundException(
                                "Category không tồn tại với id: " + bookRequestDTO.getCategoryId()));
                existingBook.setCategory(category);
            }

            bookRepository.save(existingBook);
        }
    }

    // Xoa sach theo danh sach id
    @Override
    @Transactional
    public void deleteBook(List<Long> ids) {
        bookRepository.deleteByIdIn(ids);
    }
}
