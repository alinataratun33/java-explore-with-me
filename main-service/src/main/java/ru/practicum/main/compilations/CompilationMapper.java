package ru.practicum.main.compilations;

import ru.practicum.main.compilations.dto.CompilationDto;
import ru.practicum.main.compilations.dto.NewCompilationDto;

import java.util.ArrayList;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation) {
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());
        compilationDto.setTitle(compilation.getTitle());
        compilationDto.setPinned(compilation.getPinned());
        compilationDto.setEvents(new ArrayList<>());
        return compilationDto;
    }


    public static Compilation toCompilation(NewCompilationDto compilationDto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(compilationDto.getTitle());
        compilation.setPinned(compilationDto.getPinned() != null ? compilationDto.getPinned() : false);
        compilation.setEvents(new ArrayList<>());
        return compilation;
    }
}
