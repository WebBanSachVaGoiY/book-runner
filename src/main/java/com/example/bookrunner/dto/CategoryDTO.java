package com.example.bookrunner.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CategoryDTO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private LocalDateTime createdAt;

}
