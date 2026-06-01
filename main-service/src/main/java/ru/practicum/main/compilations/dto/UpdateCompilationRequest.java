package ru.practicum.main.compilations.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    private List<Long> events;
    @Size(min = 1, max = 50)
    private String title;
    private Boolean pinned;
}