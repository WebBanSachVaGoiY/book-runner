package com.example.bookrunner.controller;

import com.example.bookrunner.dto.CategoryDTO;
import com.example.bookrunner.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryAPI {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDTO> findAll(){
        return categoryService.findAll();
    }

    @PostMapping
    public ResponseEntity<String> createCategory(@RequestBody CategoryDTO categoryDTO){
        categoryService.createCategory(categoryDTO);
        return ResponseEntity.ok().body("Thêm danh mục mới thành công!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO){
        categoryService.updateCategory(id,categoryDTO);
        return ResponseEntity.ok().body("Cập nhật danh mục thành công!");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteCategory(@RequestBody List<Long> ids){
        categoryService.deleteByIdIn(ids);
        return ResponseEntity.ok().body("Xoá danh mục thành công!");
    }
}
