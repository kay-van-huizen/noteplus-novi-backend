package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategoryId(UUID parentCategoryId);
}
