package com.example.bookrunner.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookRequestDTO {

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

    private Long categoryId;
}
