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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "price", "averageRating", "totalReviews", "createdAt", "soldCount"
    );

    // Lấy tất cả các sách (có lọc + phân trang)
    @Override
    @Transactional(readOnly = true)
    public Page<BookResponseDTO> findAll(String keyword, Long categoryId,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          int page, int size,
                                          String sortBy, String sortDir) {
        // 1. Kiểm tra và chuẩn hóa sortBy và sortDir chống lỗi 500 do sai property
        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);
        Sort sort = isAsc ? Sort.by(validSortBy).ascending() : Sort.by(validSortBy).descending();

        // 2. Tạo Pageable (page 0-indexed trong Spring Data)
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Gọi repository query kết hợp tìm kiếm + lọc (phân trang tại DB với LEFT JOIN FETCH)
        Page<Book> bookPage = bookRepository.searchAndFilterBooks(
                keyword, categoryId, minPrice, maxPrice, pageable
        );

        // 4. Chuyển đổi Page<Book> → Page<BookResponseDTO> an toàn, zero entity exposure
        return bookPage.map(this::mapToDTO);
    }

    // Lấy chi tiết sách theo id (kèm FETCH category)
    @Override
    @Transactional(readOnly = true)
    public BookResponseDTO findById(Long id) {
        Book book = bookRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + id));
        return mapToDTO(book);
    }

    private BookResponseDTO mapToDTO(Book book) {
        BookResponseDTO dto = mapper.map(book, BookResponseDTO.class);
        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getId());
            dto.setCategoryName(book.getCategory().getName());
            com.example.bookrunner.dto.CategoryDTO catDTO = mapper.map(book.getCategory(), com.example.bookrunner.dto.CategoryDTO.class);
            dto.setCategory(catDTO);
        }
        return dto;
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
        if (newBook.getSoldCount() == null)
            newBook.setSoldCount(0);
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

    @Override
    @Transactional
    public BookResponseDTO toggleFeatured(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + id));
        book.setIsFeatured(book.getIsFeatured() == null || !book.getIsFeatured());
        Book saved = bookRepository.save(book);
        return mapper.map(saved, BookResponseDTO.class);
    }
}
