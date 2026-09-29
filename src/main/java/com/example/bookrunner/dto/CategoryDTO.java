package com.example.bookrunner.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDTO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private LocalDateTime createdAt;

}
