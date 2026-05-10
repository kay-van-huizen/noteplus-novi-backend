package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;
import org.noteplus.noteplus.entity.CategoryStatus;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse getById(UUID id);

    List<CategoryResponse> getAll();

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getChildren(UUID parentId);

    CategoryResponse update(UUID id, UpdateCategoryRequest request);

    CategoryResponse updateStatus(UUID id, CategoryStatus status);

    void delete(UUID id);
}
