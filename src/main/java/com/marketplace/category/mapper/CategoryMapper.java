package com.marketplace.category.mapper;

import com.marketplace.category.dto.response.CategoryResponse;
import com.marketplace.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category){
        CategoryResponse response=new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getParent() == null
                ? null
                : category.getParent().getId()

        );
        return response;

    }
}
