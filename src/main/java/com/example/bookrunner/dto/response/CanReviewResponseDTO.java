package com.example.bookrunner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanReviewResponseDTO {
    private boolean canReview;
    private boolean alreadyReviewed;
    private boolean hasPurchased;
    private String reason;
}
