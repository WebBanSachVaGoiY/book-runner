package com.example.bookrunner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookRequestDTO {

    @NotBlank(message = "Tên sách không được để trống")
    private String title;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String author;

    private String publisher;

    private Integer publicationYear;

    private String isbn;

    private String description;

    @NotNull(message = "Giá sách không được để trống")
    @Positive(message = "Giá sách phải lớn hơn 0")
    private BigDecimal price;

    @PositiveOrZero(message = "Giá khuyến mãi không được âm")
    private BigDecimal discountPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @PositiveOrZero(message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    private String coverImageUrl;

    private Integer pageCount;

    private String language;

    private Long categoryId;

    private Boolean isFeatured;

    private Boolean active;

    private Integer soldCount;
}
