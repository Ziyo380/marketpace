package com.marketplace.category.controller;

import com.marketplace.category.dto.request.CreateCategoryRequest;
import com.marketplace.category.dto.request.UpdateCategoryRequest;
import com.marketplace.category.dto.response.CategoryResponse;
import com.marketplace.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request){
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@Valid @PathVariable String slug){
        CategoryResponse response = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping()
    public ResponseEntity<List<CategoryResponse>> getAllCategories(){
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request){
        CategoryResponse response=categoryService.updateCategory(id,request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryResponse>> getAllChildren(@PathVariable UUID parentId){
        List<CategoryResponse> children=categoryService.getChildCategories(parentId);
        return ResponseEntity.status(HttpStatus.OK).body(children);
    }

    @GetMapping("/roots")
    public ResponseEntity<List<CategoryResponse>> getRootCategories(){
        List<CategoryResponse> roots=categoryService.getRootCategories();
        return ResponseEntity.status(HttpStatus.OK).body(roots);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable UUID id){
        categoryService.deactivateCategory(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
