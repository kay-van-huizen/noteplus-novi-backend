---
description: Builds the Category backend — repository, DTOs, service interface, serviceImpl, and REST controller with role-based access for NotePlus.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Category Backend — NotePlus

## Step 0 — Read context first

Before writing any code, read these files:
- `CLAUDE.md` — architecture rules and naming conventions
- `src/main/java/org/noteplus/noteplus/entity/Category.java` — exact fields, enums, self-referential relationship
- `src/main/java/org/noteplus/noteplus/entity/BaseEntity.java` — confirm id is Long IDENTITY
- `src/main/java/org/noteplus/noteplus/entity/User.java` — for ownership checks
- `src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java`
- `src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java`
- `src/main/java/org/noteplus/noteplus/service/AuthService.java` — use as interface pattern reference
- `src/main/java/org/noteplus/noteplus/controller/AuthController.java` — use as controller pattern reference
- `src/main/java/org/noteplus/noteplus/repository/UserRepository.java` — use as repository pattern reference

## Step 1 — Git branch

```bash
git checkout main
git pull origin main
git checkout -b feat/category-crud
```

## Step 2 — Repository

Create `src/main/java/org/noteplus/noteplus/repository/CategoryRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
}
```

## Step 3 — DTOs

Read the CategoryColor and CategoryStatus enums from the Category entity before writing these.

`src/main/java/org/noteplus/noteplus/dto/request/CreateCategoryRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.noteplus.noteplus.entity.CategoryColor;
import org.noteplus.noteplus.entity.CategoryStatus;

public record CreateCategoryRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    CategoryColor color,
    CategoryStatus status,
    Long parentId   // nullable — null means root category
) {}
```

`src/main/java/org/noteplus/noteplus/dto/request/UpdateCategoryRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.noteplus.noteplus.entity.CategoryColor;
import org.noteplus.noteplus.entity.CategoryStatus;

public record UpdateCategoryRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    CategoryColor color,
    CategoryStatus status
) {}
```

`src/main/java/org/noteplus/noteplus/dto/response/CategoryResponse.java`:
```java
package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String color,
    String status,
    Long parentId,
    String parentName,
    LocalDateTime createdAt
) {}
```

## Step 4 — Service interface

`src/main/java/org/noteplus/noteplus/service/CategoryService.java`:
```java
package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CreateCategoryRequest request, String username);
    CategoryResponse getById(Long id);
    List<CategoryResponse> getAll();
    List<CategoryResponse> getChildren(Long parentId);
    CategoryResponse update(Long id, UpdateCategoryRequest request, String username);
    void delete(Long id, String username);
}
```

## Step 5 — Service implementation

`src/main/java/org/noteplus/noteplus/service/impl/CategoryServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `CategoryRepository`, `UserRepository`

Implement each method:

**create():**
- Load user by username — throw ResourceNotFoundException("User not found") if absent
- If parentId is not null: load parent from CategoryRepository — throw ResourceNotFoundException("Parent category not found") if absent
- Build new Category entity, set name/color/status/user/parent
- Save and return toResponse(saved)

**getById():**
- Find by id — throw ResourceNotFoundException("Category not found: " + id) if absent
- Return toResponse(category)

**getAll():**
- Return categoryRepository.findAll() mapped to toResponse()

**getChildren():**
- Return categoryRepository.findByParentId(parentId) mapped to toResponse()

**update():**
- Load category — throw ResourceNotFoundException if absent
- Ownership check: if NOT category.getUser().getUsername().equals(username) throw ForbiddenException("You do not own this category")
- Apply name/color/status from request, save, return toResponse()

**delete():**
- Load category — throw ResourceNotFoundException if absent
- Ownership check — throw ForbiddenException if not owner
- Check for children: if !categoryRepository.findByParentId(id).isEmpty() throw ForbiddenException("Cannot delete a category that has subcategories")
- Delete

**private CategoryResponse toResponse(Category c):**
```java
return new CategoryResponse(
    c.getId(),
    c.getName(),
    c.getColor() != null ? c.getColor().name() : null,
    c.getStatus() != null ? c.getStatus().name() : null,
    c.getParent() != null ? c.getParent().getId() : null,
    c.getParent() != null ? c.getParent().getName() : null,
    c.getCreatedAt()
);
```

## Step 6 — REST Controller

Role rules to enforce via @PreAuthorize:
- ADMIN: full access to all endpoints
- COACH: can create and update, cannot delete
- STUDENT: read-only (GET endpoints only)

`src/main/java/org/noteplus/noteplus/controller/CategoryController.java`:

```java
package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.dto.request.UpdateCategoryRequest;
import org.noteplus.noteplus.dto.response.CategoryResponse;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management — ADMIN/COACH can create and update, only ADMIN can delete, all roles can read")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child categories of a category")
    @ApiResponse(responseCode = "200", description = "Children retrieved")
    public ResponseEntity<List<CategoryResponse>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getChildren(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Create a new category")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Students cannot create categories")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Update a category")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "403", description = "Not your category or insufficient role")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            Authentication auth) {
        return ResponseEntity.ok(categoryService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category — ADMIN only")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "403", description = "Only ADMIN can delete categories")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        categoryService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
```

## Step 7 — Compile check

If it compiles clean, start the app and verify via Swagger (http://localhost:8080/swagger-ui/index.html):

| Test | Token role | Expected |
|------|-----------|----------|
| GET /api/categories | any | 200 |
| POST /api/categories | STUDENT token | 403 |
| POST /api/categories | COACH token | 201 |
| DELETE /api/categories/{id} | COACH token | 403 |
| DELETE /api/categories/{id} | ADMIN token | 204 |

Do NOT proceed to category-frontend.md until all compile and Swagger tests pass.
