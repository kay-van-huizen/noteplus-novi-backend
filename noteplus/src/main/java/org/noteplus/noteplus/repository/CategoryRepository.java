package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategoryId(Long parentCategoryId);
}
