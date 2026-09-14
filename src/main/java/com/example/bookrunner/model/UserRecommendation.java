package com.example.bookrunner.model;

import com.example.bookrunner.enums.RecommendationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_recommendations",
        indexes = {
                @Index(name = "idx_user_type_rank", columnList = "user_id, recommendation_type, rank_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRecommendation.UserRecommendationId.class)
public class UserRecommendation {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", length = 30)
    @Builder.Default
    private RecommendationType recommendationType = RecommendationType.FOR_YOU;

    @Column(length = 50)
    private String algorithm; // 'ALS', 'CONTENT_BASED', 'HYBRID'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private Book book;

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
    public static class UserRecommendationId implements Serializable {
        private Long userId;
        private Long bookId;
        private RecommendationType recommendationType;
    }
}