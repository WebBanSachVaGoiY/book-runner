package com.example.bookrunner.repository;

import com.example.bookrunner.model.BookSimilarity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookSimilarityRepository extends JpaRepository<BookSimilarity, BookSimilarity.BookSimilarityId> {

    List<BookSimilarity> findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
            Long bookId1,
            String similarityType,
            Pageable pageable
    );

    List<BookSimilarity> findByBookId1OrderByRankOrderAsc(Long bookId1, Pageable pageable);

    boolean existsByBookId1(Long bookId1);

    void deleteByBookId1(Long bookId1);

    void deleteByBookId1AndSimilarityType(Long bookId1, String similarityType);
}
