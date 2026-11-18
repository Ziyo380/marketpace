package com.marketplace.category.service;

import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.category.dto.request.CreateCategoryRequest;
import com.marketplace.category.dto.request.UpdateCategoryRequest;
import com.marketplace.category.dto.response.CategoryResponse;
import com.marketplace.category.entity.Category;
import com.marketplace.category.mapper.CategoryMapper;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService{
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request){
        String slug=request.slug();
        boolean exists=categoryRepository.existsBySlug(slug);
        if(exists){
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }
        UUID parentId= request.parentId();
        Category parent=null;
        if(parentId!=null){
            parent=categoryRepository.findById(parentId)
                    .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + parentId + "' was not found"));
            if (!parent.isActive()){
                throw new BusinessRuleException("Parent category with ID '" + parent.getId() + "' is inactive");
            }
        }

        Category category=new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setParent(parent);
        Category savedCategory=categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug){
        Category category=categoryRepository.findBySlug(slug)
                .orElseThrow(()-> new ResourceNotFoundException("Category with slug '" + slug + "' was not found"));
        if(!category.isActive()){
           throw new ResourceNotFoundException("Category with slug '" + slug + "' is inactive");
        }else {
            categoryMapper.toResponse(category);
        }
        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(){
        List<Category> categories=categoryRepository.findAllByActiveTrueOrderByNameAsc();
        List<CategoryResponse> response=categories
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
        return response;

    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request){
        Category category=categoryRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + id + "' was not found"));

        if (!request.slug().equals(category.getSlug())){
            boolean exists=categoryRepository.existsBySlug(request.slug());
            if(exists){
                throw new ConflictException("A category with slug '" + request.slug() + "' already exists");
            }
        }
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        if (!request.active() && category.isActive()){
            boolean hasActiveChildren=categoryRepository.existsByParent_IdAndActiveTrue(category.getId());
            if (hasActiveChildren){
                throw new BusinessRuleException("Cannot deactivate this category while it has active child categories");
            }
        }
        category.setActive(request.active());
        UUID parentId= request.parentId();
        if(parentId==null){
            category.setParent(null);
        }else {
            if (parentId.equals(category.getId())) {
                throw new BusinessRuleException("A category cannot be its own parent");
            }

            Category newParent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category with ID '" + parentId + "' was not found"));
            if (!newParent.isActive()) {
                throw new BusinessRuleException("Parent category with ID '" + newParent.getId() + "' is inactive");
            }
            Category curent = newParent;
            while (curent != null) {
                if (curent.getId().equals(category.getId())) {
                    throw new BusinessRuleException("The selected parent would create a cycle in the category hierarchy");
                }
                curent = curent.getParent();
            }
            category.setParent(newParent);
        }

        Category savedCategory=categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(UUID parentId){
        Category parent=categoryRepository.findById(parentId)
                .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + parentId + "' was not found"));
        boolean active=parent.isActive();
        if (!active){
            throw new ResourceNotFoundException("Category with ID '" + parentId + "' is inactive");
        }
        List<Category> children=categoryRepository.findAllByParent_IdAndActiveTrueOrderByNameAsc(parentId);
        return children
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories(){
        List<Category> roots=categoryRepository.findAllByParentIsNullAndActiveTrueOrderByNameAsc();
        return roots
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deactivateCategory(UUID id){
        Category category=categoryRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + id + "' was not found"));
        if (!category.isActive()){
            return;
        }
        boolean exists=categoryRepository.existsByParent_IdAndActiveTrue(category.getId());
        if (exists){
            throw new BusinessRuleException("Cannot deactivate this category while it has active child categories");
        }
        category.setActive(false);
    }


}
