package com.example.bookrunner.dto.response;

import com.example.bookrunner.model.Category;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookResponseDTO {

    private String title;

    private String author;

    private String publisher;

    private Integer publicationYear;

    private String isbn;

    private String description;

    private BigDecimal price;

    private BigDecimal discountPrice;

    private Integer stockQuantity;

    private String coverImageUrl;

    private Integer pageCount;

    private String language;

    private Double averageRating;

    private Integer totalReviews;

    private Category category;

    private LocalDateTime createdAt;
}
