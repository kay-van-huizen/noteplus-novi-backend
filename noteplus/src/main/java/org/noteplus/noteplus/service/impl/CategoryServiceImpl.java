package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;
import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.CategoryColor;
import org.noteplus.noteplus.entity.CategoryStatus;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.exception.ValidationException;
import org.noteplus.noteplus.repository.CategoryRepository;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final NoteRepository noteRepository;

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildren(UUID parentId) {
        return categoryRepository.findByParentCategoryId(parentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        var category = findOrThrow(id);

        category.setTitle(request.title());
        category.setDescription(request.description());
        if (request.color() != null) category.setColor(request.color());
        if (request.status() != null) category.setStatus(request.status());

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new ValidationException("A category cannot be its own parent");
            }
            var parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.parentId()));
            category.setParentCategory(parent);
        }
        // null parentId → keep existing parentCategory unchanged

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var category = findOrThrow(id);

        if (!categoryRepository.findByParentCategoryId(id).isEmpty()) {
            throw new ValidationException("Cannot delete a category that has subcategories");
        }

        if (!noteRepository.findByCategoryIdNotDeleted(id).isEmpty()) {
            throw new ValidationException("Category has notes attached — reassign or delete the notes first");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateStatus(UUID id, CategoryStatus status) {
        var category = findOrThrow(id);
        category.setStatus(status);
        return toResponse(categoryRepository.save(category));
    }

    private Category findOrThrow(UUID id) {
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
