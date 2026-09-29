package com.example.bookrunner.dto.response;

import com.example.bookrunner.dto.CategoryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookResponseDTO {

    private Long id;

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

    private Integer soldCount;

    private Boolean isFeatured;

    private Boolean active;

    private Long categoryId;

    private String categoryName;

    private CategoryDTO category;

    private LocalDateTime createdAt;
}
