package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.PatchCategoryStatusRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "ADMIN/COACH can create and update, only ADMIN can delete, all roles can read")
//@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    @ApiResponse(responseCode = "200", description = "Categories retrieved")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root categories (no parent)")
    @ApiResponse(responseCode = "200", description = "Root categories retrieved")
    public ResponseEntity<List<CategoryResponse>> getRoots() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child categories")
    @ApiResponse(responseCode = "200", description = "Children retrieved")
    public ResponseEntity<List<CategoryResponse>> getChildren(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getChildren(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Create a category")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Insufficient role")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Update a category")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Insufficient role")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Update category status (ACTIVE / INACTIVE)")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "403", description = "Insufficient role")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryResponse> patchStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PatchCategoryStatusRequest request) {
        return ResponseEntity.ok(categoryService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category — ADMIN only")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "403", description = "Has subcategories or insufficient role")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
