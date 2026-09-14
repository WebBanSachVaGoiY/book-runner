package com.example.bookrunner.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "book_similarities",
        indexes = {
                @Index(name = "idx_book_similarity_lookup", columnList = "book_id_1, similarity_type, rank_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(BookSimilarity.BookSimilarityId.class)
public class BookSimilarity {

    @Id
    @Column(name = "book_id_1")
    private Long bookId1;

    @Id
    @Column(name = "book_id_2")
    private Long bookId2;

    @Id
    @Column(name = "similarity_type", length = 50)
    private String similarityType; // 'CONTENT_BASED', 'ALSO_BOUGHT', 'SAME_AUTHOR'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id_1", insertable = false, updatable = false)
    private Book sourceBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id_2", insertable = false, updatable = false)
    private Book similarBook;

    @Column(nullable = false)
    private Float score;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Composite Key Class
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class BookSimilarityId implements Serializable {
        private Long bookId1;
        private Long bookId2;
        private String similarityType;
    }
}
