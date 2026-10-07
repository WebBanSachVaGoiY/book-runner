package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.CategoryDTO;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.DuplicateUniqueFieldException;
import com.example.bookrunner.exception.FieldRequiredException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CategoryRepository;
import com.example.bookrunner.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    @Override
    public List<CategoryDTO> findAll(){
        List<Category> categoryList = categoryRepository.findAll();
        return categoryList.stream().map(response->mapper.map(response, CategoryDTO.class)).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getName() == null || categoryDTO.getName().isBlank()) throw new FieldRequiredException("Vui lòng nhập đủ thông tin!");
        if (categoryDTO.getSlug() != null && categoryRepository.findBySlug(categoryDTO.getSlug()).isPresent()) {
            throw new DuplicateUniqueFieldException("Slug bị trùng!");
        }
        Category newCategory = mapper.map(categoryDTO, Category.class);
        newCategory.setId(null);
        categoryRepository.save(newCategory);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCategory(Long id, CategoryDTO categoryDTO) {
        if (categoryDTO.getName() == null || categoryDTO.getName().isBlank()) throw new FieldRequiredException("Vui lòng nhập đủ thông tin!");
        if (categoryDTO.getSlug() != null) {
            categoryRepository.findBySlug(categoryDTO.getSlug()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new DuplicateUniqueFieldException("Slug bị trùng!");
                }
            });
        }
        Optional<Category> updating = categoryRepository.findById(id);
        if (updating.isEmpty()) {
            throw new ItemNotFoundException("Danh mục không tồn tại!");
        } else {
            Category currentCategory = updating.get();
            mapper.map(categoryDTO, currentCategory);
            currentCategory.setId(id);
            categoryRepository.save(currentCategory);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Danh sách ID danh mục cần xóa không được để trống!");
        }
        if (bookRepository.existsByCategoryIdIn(ids)) {
            throw new BadRequestException("Không thể xóa danh mục đang có sách. Vui lòng chuyển sách sang danh mục khác trước khi xóa!");
        }
        categoryRepository.deleteByIdIn(ids);
    }

}
