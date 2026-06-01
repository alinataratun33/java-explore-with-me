package ru.practicum.main.categories;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.categories.dto.CategoryDto;
import ru.practicum.main.categories.dto.NewCategoryDto;
import ru.practicum.main.events.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена с ID: " + id));
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение всех категорий: from={}, size={}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Category> categories = categoryRepository.findAll(pageable);

        return categories.getContent().stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        log.info("Поиск категории по ID: {}", id);

        Category category = getCategoryOrThrow(id);
        log.info("Категория найдена: {}", category.getName());
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории с названием: {}", newCategoryDto.getName());

        checkNameUniqueness(newCategoryDto.getName());
        Category category = CategoryMapper.toCategory(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Категория успешно создана");
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        log.info("Обновление категории ID {}: {}", id, categoryDto.getName());

        Category existingCategory = getCategoryOrThrow(id);
        if (!categoryDto.getName().equals(existingCategory.getName())) {
            checkNameUniqueness(categoryDto.getName());
            existingCategory.setName(categoryDto.getName());
        }

        return CategoryMapper.toCategoryDto(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Удаление категории с ID: {}", id);

        getCategoryOrThrow(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("Нельзя удалить категорию, так как есть события с этой категорией");
        }
        categoryRepository.deleteById(id);

        log.info("Категория удалена с ID: {}", id);
    }

    private void checkNameUniqueness(String name) {
        if (categoryRepository.existsByName(name)) {
            log.warn("Попытка создать/обновить категорию с уже существующим названием: {}", name);
            throw new ConflictException("Название уже существует: " + name);
        }
    }
}
