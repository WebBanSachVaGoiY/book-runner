package com.example.bookrunner.repository;

import com.example.bookrunner.enums.RecommendationType;
import com.example.bookrunner.model.UserRecommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, UserRecommendation.UserRecommendationId> {

    List<UserRecommendation> findByUserIdAndRecommendationTypeOrderByRankOrderAsc(
            Long userId,
            RecommendationType recommendationType,
            Pageable pageable
    );

    List<UserRecommendation> findByUserIdOrderByRankOrderAsc(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndRecommendationType(Long userId, RecommendationType recommendationType);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndRecommendationType(Long userId, RecommendationType recommendationType);
}
