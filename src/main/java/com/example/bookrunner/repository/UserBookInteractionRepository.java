package com.example.bookrunner.repository;

import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.model.UserBookInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBookInteractionRepository extends JpaRepository<UserBookInteraction, Long> {

    Page<UserBookInteraction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByBookId(Long bookId);

    Boolean existsByUserIdAndBookIdAndInteractionType(Long userId, Long bookId, InteractionType interactionType);

    boolean existsByUserIdAndBookIdAndInteractionTypeAndCreatedAtAfter(
            Long userId, Long bookId, InteractionType interactionType, java.time.LocalDateTime afterTime);

    @Query("SELECT DISTINCT ubi.book.id FROM UserBookInteraction ubi WHERE ubi.user.id = :userId")
    List<Long> findDistinctBookIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ubi.book.id FROM UserBookInteraction ubi WHERE ubi.user.id = :userId ORDER BY ubi.createdAt DESC")
    List<Long> findRecentInteractedBookIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    java.util.Optional<UserBookInteraction> findTopByUserIdAndBookIdAndInteractionTypeOrderByCreatedAtDesc(
            Long userId, Long bookId, InteractionType interactionType);

    void deleteByUserIdAndBookIdAndInteractionType(Long userId, Long bookId, InteractionType interactionType);
}
