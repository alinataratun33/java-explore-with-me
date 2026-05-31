package ru.practicum.main.compilations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.compilations.CompilationService;
import ru.practicum.main.compilations.dto.CompilationDto;
import ru.practicum.main.compilations.dto.NewCompilationDto;
import ru.practicum.main.compilations.dto.UpdateCompilationRequest;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {
    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("Запрос на создание подборки событий");
        return compilationService.createCompilation(newCompilationDto);
    }

    @PatchMapping("/{id}")
    public CompilationDto updateCompilation(@PathVariable Long id,
                                            @RequestBody @Valid UpdateCompilationRequest updateRequest) {
        log.info("Запрос на обновление подборки событий с ID: {}", id);
        return compilationService.updateCompilation(id, updateRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long id) {
        log.info("Запрос на удаление подборки событий с ID: {}", id);
        compilationService.deleteCompilation(id);
    }
}
