package ru.practicum.main.compilations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.compilations.dto.CompilationDto;
import ru.practicum.main.compilations.dto.NewCompilationDto;
import ru.practicum.main.compilations.dto.UpdateCompilationRequest;
import ru.practicum.main.events.Event;
import ru.practicum.main.events.EventMapper;
import ru.practicum.main.events.EventRepository;
import ru.practicum.main.events.dto.EventShortDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.requests.RequestRepository;
import ru.practicum.main.requests.RequestStatus;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    private Compilation getCompilationOrThrow(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена с ID: " + id));
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение подборок: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilationsPage;

        if (pinned != null) {
            compilationsPage = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilationsPage = compilationRepository.findAll(pageable);
        }

        List<Compilation> compilations = compilationsPage.getContent();
        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        return compilations.stream()
                .map(this::enrichWithStats)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long id) {
        log.info("Получение подборки по ID: {}", id);

        Compilation compilation = getCompilationOrThrow(id);

        return enrichWithStats(compilation);
    }


    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание подборки: {}", newCompilationDto.getTitle());

        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(newCompilationDto.getEvents());
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        return enrichWithStats(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с ID: {}", id);

        Compilation compilation = getCompilationOrThrow(id);

        if (updateRequest.getEvents() != null) {
            List<Event> events;
            if (updateRequest.getEvents().isEmpty()) {
                events = new ArrayList<>();
            } else {
                events = eventRepository.findAllById(updateRequest.getEvents());
            }
            compilation.setEvents(events);
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            compilation.setTitle(updateRequest.getTitle());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return enrichWithStats(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long id) {
        log.info("Удаление подборки с ID: {}", id);

        getCompilationOrThrow(id);

        compilationRepository.deleteById(id);
    }

    private CompilationDto enrichWithStats(Compilation compilation) {
        List<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            return CompilationMapper.toDto(compilation);
        }

        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);

        CompilationDto dto = CompilationMapper.toDto(compilation);

        List<EventShortDto> enrichedEvents = compilation.getEvents().stream()
                .map(event -> EventMapper.toEventShortDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        dto.setEvents(enrichedEvents);
        return dto;
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        return requestRepository
                .countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            ResponseEntity<Object> response = statsClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now(),
                    uris,
                    true
            );

            if (response.getBody() == null) {
                return new HashMap<>();
            }

            List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(), new TypeReference<>() {
            });

            Map<Long, Long> viewsMap = new HashMap<>();
            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                Long id = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                viewsMap.put(id, stat.getHits());
            }
            return viewsMap;
        } catch (Exception e) {
            log.error("Ошибка получения просмотров для списка событий", e);
            return new HashMap<>();
        }
    }
}