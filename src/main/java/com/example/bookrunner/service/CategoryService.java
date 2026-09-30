package com.example.bookrunner.service;

import com.example.bookrunner.dto.CategoryDTO;
import com.example.bookrunner.exception.DuplicateUniqueFieldException;
import com.example.bookrunner.exception.FieldRequiredException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper mapper;

    public List<CategoryDTO> findAll(){
        List<Category> categoryList = categoryRepository.findAll();
        return categoryList.stream().map(response->mapper.map(response, CategoryDTO.class)).collect(Collectors.toList());
    }

    public void createCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getName()==null|| categoryDTO.getName().isBlank()) throw new FieldRequiredException("Vui lòng nhập đủ thông tin!");
        if (categoryRepository.findBySlug(categoryDTO.getSlug()).isPresent()) throw new DuplicateUniqueFieldException("Slug bị trùng!");
        Category newCategory = mapper.map(categoryDTO, Category.class);
        categoryRepository.save(newCategory);
    }

    public void updateCategory(Long id, CategoryDTO categoryDTO) {
        if (categoryDTO.getName()==null|| categoryDTO.getName().isBlank()) throw new FieldRequiredException("Vui lòng nhập đủ thông tin!");
        if (categoryRepository.findBySlug(categoryDTO.getSlug()).isPresent()) throw new DuplicateUniqueFieldException("Slug bị trùng!");
        Optional<Category> updating = categoryRepository.findById(id);
        if (updating.isEmpty()){
            throw new ItemNotFoundException("Danh muc khong ton tai!");
        }
        else{
            Category currentCategory = updating.get();
            mapper.map(categoryDTO, currentCategory);
            categoryRepository.save(currentCategory);
        }
    }

    public void deleteByIdIn(List<Long> ids) {
        categoryRepository.deleteByIdIn(ids);
    }

}
