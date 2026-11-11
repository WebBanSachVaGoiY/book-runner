package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.InteractionLogRequest;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.User;
import com.example.bookrunner.model.UserBookInteraction;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.impl.InteractionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private UserBookInteractionRepository interactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private User sampleUser;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        interactionService.clearDebounceCache();

        sampleUser = User.builder()
                .id(1L)
                .username("testcustomer")
                .email("test@example.com")
                .build();

        sampleBook = Book.builder()
                .id(100L)
                .title("Clean Code")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Ghi nhận tương tác VIEW thành công với weight 1.0")
    void testLogInteraction_View_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        InteractionLogRequest request = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.VIEW)
                .build();

        interactionService.logInteraction(1L, request);

        ArgumentCaptor<UserBookInteraction> captor = ArgumentCaptor.forClass(UserBookInteraction.class);
        verify(interactionRepository, times(1)).save(captor.capture());

        UserBookInteraction saved = captor.getValue();
        assertEquals(sampleUser, saved.getUser());
        assertEquals(sampleBook, saved.getBook());
        assertEquals(InteractionType.VIEW, saved.getInteractionType());
        assertEquals(1.0f, saved.getWeight());
    }

    @Test
    @DisplayName("Ghi nhận tương tác ADD_TO_CART thành công với weight 3.0")
    void testLogInteraction_AddToCart_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        InteractionLogRequest request = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.ADD_TO_CART)
                .build();

        interactionService.logInteraction(1L, request);

        ArgumentCaptor<UserBookInteraction> captor = ArgumentCaptor.forClass(UserBookInteraction.class);
        verify(interactionRepository, times(1)).save(captor.capture());

        UserBookInteraction saved = captor.getValue();
        assertEquals(InteractionType.ADD_TO_CART, saved.getInteractionType());
        assertEquals(3.0f, saved.getWeight());
    }

    @Test
    @DisplayName("Atomic Debounce: F5 liên tục trong cửa sổ TTL chỉ ghi nhận duy nhất 1 lần")
    void testLogInteraction_Debounce_SkipsDuplicates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        InteractionLogRequest request = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.VIEW)
                .build();

        // Lần 1: Lưu thành công
        interactionService.logInteraction(1L, request);
        // Lần 2 & 3: Gọi liên tiếp ngay lập tức -> Phải bị debounce chặn
        interactionService.logInteraction(1L, request);
        interactionService.logInteraction(1L, request);

        // Chỉ được gọi repository đúng 1 lần
        verify(interactionRepository, times(1)).save(any(UserBookInteraction.class));
    }

    @Test
    @DisplayName("Bỏ qua không ghi interaction nếu sách không tồn tại")
    void testLogInteraction_BookNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        InteractionLogRequest request = InteractionLogRequest.builder()
                .bookId(999L)
                .interactionType(InteractionType.VIEW)
                .build();

        interactionService.logInteraction(1L, request);

        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bỏ qua không ghi interaction nếu sách đã ngừng kinh doanh (active = false)")
    void testLogInteraction_InactiveBook() {
        sampleBook.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        InteractionLogRequest request = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.VIEW)
                .build();

        interactionService.logInteraction(1L, request);

        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Từ chối ghi nhận hành vi BUY hoặc RATING qua API tương tác client")
    void testLogInteraction_RejectBuyAndRating() {
        InteractionLogRequest buyRequest = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.BUY)
                .build();

        InteractionLogRequest ratingRequest = InteractionLogRequest.builder()
                .bookId(100L)
                .interactionType(InteractionType.RATING)
                .build();

        interactionService.logInteraction(1L, buyRequest);
        interactionService.logInteraction(1L, ratingRequest);

        verify(interactionRepository, never()).save(any());
    }
}
