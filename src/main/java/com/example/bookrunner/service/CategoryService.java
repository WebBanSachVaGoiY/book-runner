package com.example.bookrunner.service;

import com.example.bookrunner.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> findAll();
    void createCategory(CategoryDTO categoryDTO);
    void updateCategory(Long id, CategoryDTO categoryDTO);
    void deleteByIdIn(List<Long> ids);
}
