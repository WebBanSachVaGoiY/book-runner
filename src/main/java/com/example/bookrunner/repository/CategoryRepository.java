package com.example.bookrunner.repository;

import com.example.bookrunner.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    Boolean existsBySlug(String slug);

    Boolean existsByName(String name);

    void deleteByIdIn(List<Long> ids);
}
