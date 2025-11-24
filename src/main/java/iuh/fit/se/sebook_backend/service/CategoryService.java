package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.CategoryDTO;
import iuh.fit.se.sebook_backend.entity.Category;
import iuh.fit.se.sebook_backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setName(categoryDTO.getName());
        Category savedCategory = categoryRepository.save(category);
        return toDto(savedCategory);
    }

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        return toDto(category);
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        category.setName(categoryDTO.getName());
        Category updatedCategory = categoryRepository.save(category);
        return toDto(updatedCategory);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDTO toDto(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }
}