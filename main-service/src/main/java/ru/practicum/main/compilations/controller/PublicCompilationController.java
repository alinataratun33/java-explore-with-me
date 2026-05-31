package ru.practicum.main.compilations.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.compilations.CompilationService;
import ru.practicum.main.compilations.dto.CompilationDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("compilations")
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") int from,
                                                @RequestParam(defaultValue = "10") int size) {
        log.info("Запрос на получение подборок");
        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/{id}")
    public CompilationDto getCompilationById(@PathVariable Long id) {
        log.info("Запрос на получение подборки с ID: {}", id);
        return compilationService.getCompilationById(id);
    }
}
