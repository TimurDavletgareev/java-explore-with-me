package ru.practicum.ewm.category.dto;

import ru.practicum.ewm.category.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryMapper {

    public static CategoryDto categoryToDto(Category category) {

        return new CategoryDto(category.getId(), category.getName());
    }

    public static List<CategoryDto> categoryToDto(Iterable<Category> categories) {

        List<CategoryDto> listToReturn = new ArrayList<>();

        for (Category category : categories) {
            listToReturn.add(categoryToDto(category));
        }

        return listToReturn;
    }

    public static Category categoryDtoToModel(CategoryDto categoryDto) {

        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setName(categoryDto.getName());

        return category;
    }
}
