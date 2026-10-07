package com.example.bookrunner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDTO {

    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String slug;

    private String description;

    private LocalDateTime createdAt;

}
