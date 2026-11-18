package com.marketplace.category.repository;

import com.marketplace.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByActiveTrueOrderByNameAsc();

    boolean existsByParent_IdAndActiveTrue(UUID parentId);

    List<Category> findAllByParent_IdAndActiveTrueOrderByNameAsc(UUID parentId);

    List<Category> findAllByParentIsNullAndActiveTrueOrderByNameAsc();
}
