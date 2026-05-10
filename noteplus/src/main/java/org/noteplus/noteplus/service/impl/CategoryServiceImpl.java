package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;
import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.CategoryColor;
import org.noteplus.noteplus.entity.CategoryStatus;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.CategoryRepository;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.parentId()));
        }

        var category = new Category();
        category.setTitle(request.title());
        category.setDescription(request.description());
        category.setColor(request.color() != null ? request.color() : CategoryColor.DEFAULT);
        category.setStatus(request.status() != null ? request.status() : CategoryStatus.ACTIVE);
        category.setParentCategory(parent);

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getChildren(Long parentId) {
        return categoryRepository.findByParentCategoryId(parentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        var category = findOrThrow(id);

        category.setTitle(request.title());
        category.setDescription(request.description());
        if (request.color() != null) category.setColor(request.color());
        if (request.status() != null) category.setStatus(request.status());

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        var category = findOrThrow(id);

        if (!categoryRepository.findByParentCategoryId(id).isEmpty()) {
            throw new ForbiddenException("Cannot delete a category that has subcategories");
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getColor() != null ? c.getColor().name() : null,
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getParentCategory() != null ? c.getParentCategory().getId() : null,
                c.getParentCategory() != null ? c.getParentCategory().getTitle() : null,
                c.getCreatedAt()
        );
    }
}
