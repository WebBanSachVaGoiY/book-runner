package com.example.bookrunner.controller;

import com.example.bookrunner.dto.CategoryDTO;
import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryAPI {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> findAll(){
        return ResponseEntity.ok(ApiResponse.success(categoryService.findAll()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody CategoryDTO categoryDTO){
        categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Thêm danh mục mới thành công!", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable("id") Long id, @RequestBody CategoryDTO categoryDTO){
        categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công!", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@RequestBody List<Long> ids){
        categoryService.deleteByIdIn(ids);
        return ResponseEntity.ok(ApiResponse.success("Xoá danh mục thành công!", null));
    }
}
