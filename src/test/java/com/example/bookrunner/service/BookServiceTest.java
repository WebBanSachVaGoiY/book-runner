package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CategoryRepository;
import com.example.bookrunner.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Công nghệ thông tin")
                .slug("cong-nghe-thong-tin")
                .build();

        testBook = Book.builder()
                .id(100L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(new BigDecimal("350000"))
                .discountPrice(new BigDecimal("300000"))
                .stockQuantity(20)
                .active(true)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("findById - Thành công khi sách tồn tại")
    void findById_Success() {
        when(bookRepository.findByIdWithCategory(100L)).thenReturn(Optional.of(testBook));

        BookResponseDTO response = bookService.findById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Clean Code", response.getTitle());
        assertEquals("Robert C. Martin", response.getAuthor());
        assertEquals(new BigDecimal("350000"), response.getPrice());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Công nghệ thông tin", response.getCategoryName());
        assertNotNull(response.getCategory());
        assertEquals(1L, response.getCategory().getId());
        verify(bookRepository, times(1)).findByIdWithCategory(100L);
    }

    @Test
    @DisplayName("findById - Ném ItemNotFoundException khi không tìm thấy sách")
    void findById_NotFound() {
        when(bookRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> bookService.findById(999L));
        verify(bookRepository, times(1)).findByIdWithCategory(999L);
    }

    @Test
    @DisplayName("findAll - SortBy không hợp lệ tự động fallback về createdAt DESC")
    void findAll_InvalidSort_FallbackToCreatedAt() {
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        when(bookRepository.searchAndFilterBooks(any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(testBook)));

        bookService.findAll(null, null, null, null, 0, 10, "malicious_or_unknown_column", "desc");

        org.springframework.data.domain.Pageable captured = pageableCaptor.getValue();
        org.springframework.data.domain.Sort.Order order = captured.getSort().getOrderFor("createdAt");
        assertNotNull(order, "Phải fallback về sort theo createdAt");
        assertTrue(order.isDescending());
    }

    @Test
    @DisplayName("findAll - SortBy soldCount hợp lệ được chấp nhận")
    void findAll_ValidSoldCountSort() {
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        when(bookRepository.searchAndFilterBooks(any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(testBook)));

        bookService.findAll(null, null, null, null, 0, 10, "soldCount", "desc");

        org.springframework.data.domain.Pageable captured = pageableCaptor.getValue();
        org.springframework.data.domain.Sort.Order order = captured.getSort().getOrderFor("soldCount");
        assertNotNull(order, "Phải chấp nhận sort theo soldCount");
        assertTrue(order.isDescending());
    }

    @Test
    @DisplayName("toggleFeatured - Đảo trạng thái isFeatured thành công")
    void toggleFeatured_Success() {
        testBook.setIsFeatured(false);
        when(bookRepository.findById(100L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponseDTO response = bookService.toggleFeatured(100L);

        assertNotNull(response);
        assertTrue(response.getIsFeatured());
        verify(bookRepository, times(1)).save(testBook);
    }
}
