package ru.practicum.main.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.categories.Category;
import ru.practicum.main.categories.CategoryMapper;
import ru.practicum.main.categories.CategoryRepository;
import ru.practicum.main.categories.dto.CategoryDto;
import ru.practicum.main.events.dto.*;
import ru.practicum.main.events.enums.State;
import ru.practicum.main.events.enums.StateAction;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.locations.LocationDto;
import ru.practicum.main.requests.RequestRepository;
import ru.practicum.main.requests.RequestStatus;
import ru.practicum.main.users.User;
import ru.practicum.main.users.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private static final int MIN_HOURS_AFTER_PUBLISH = 1;

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена с ID: " + id));
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Мероприятие не найдено с ID: " + id));
    }

    private void validateEventDate(LocalDateTime eventDate, boolean isUpdate, LocalDateTime publishedOn) {
        if (eventDate == null) return;

        if (!isUpdate || publishedOn == null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
                throw new ValidationException("Дата события должна быть не ранее чем за " + MIN_HOURS_BEFORE_EVENT
                        + " часа от текущего момента");
            }
        } else if (eventDate.isBefore(publishedOn.plusHours(MIN_HOURS_AFTER_PUBLISH))) {
            throw new ConflictException("Дата события должна быть не ранее чем за " + MIN_HOURS_AFTER_PUBLISH + " от даты публикации");
        }
    }

    private void processStateAction(Event event, StateAction stateAction, boolean isAdmin) {
        if (stateAction == null) return;

        if (!isAdmin) {
            if (stateAction == StateAction.SEND_TO_REVIEW) {
                if (event.getState() == State.CANCELED) {
                    event.setState(State.PENDING);
                }
            } else if (stateAction == StateAction.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            }
        } else {
            if (stateAction == StateAction.PUBLISH_EVENT) {
                if (event.getState() != State.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
                }
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (stateAction == StateAction.REJECT_EVENT) {
                if (event.getState() == State.PUBLISHED) {
                    throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
                }
                event.setState(State.CANCELED);
            }
        }
    }

    private EventFullDto enrichEventWithStats(Event event) {
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        Long views = getViewsForEvents(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        return EventMapper.toEventFullDto(event, views, confirmedRequests);
    }

    private List<EventShortDto> mapToEventShortDtoList(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события: {}", newEventDto.getAnnotation());

        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        validateEventDate(newEventDto.getEventDate(), false, null);

        Event event = EventMapper.toEvent(newEventDto, initiator, category);
        Event savedEvent = eventRepository.save(event);

        log.info("Событие создано с ID: {}", savedEvent.getId());
        return enrichEventWithStats(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Получение всех событий пользователя с ID: {}", userId);

        getUserOrThrow(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return mapToEventShortDtoList(events.getContent());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Получение события ID: {} для пользователя ID: {}", eventId, userId);
        Event event = getEventOrThrow(eventId);
        getUserOrThrow(userId);
        return enrichEventWithStats(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события ID: {} пользователем ID: {}", eventId, userId);

        getUserOrThrow(userId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        validateEventDate(updateRequest.getEventDate(), true, event.getPublishedOn());

        CategoryDto categoryDto = null;
        if (updateRequest.getCategory() != null) {
            Category category = getCategoryOrThrow(updateRequest.getCategory());
            categoryDto = CategoryMapper.toCategoryDto(category);
        }

        LocationDto locationDto = updateRequest.getLocation();

        EventMapper.updateFromUserDto(event, updateRequest, categoryDto, locationDto);
        processStateAction(event, updateRequest.getStateAction(), false);

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие ID: {} обновлено", updatedEvent.getId());

        return enrichEventWithStats(updatedEvent);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, Integer from, Integer size) {

        List<State> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream()
                    .map(State::valueOf)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> eventsPage = eventRepository.findEventsByAdmin(
                users, stateEnums, categories, rangeStart, rangeEnd, pageable);

        if (eventsPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = eventsPage.getContent().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(eventIds);

        return eventsPage.getContent().stream()
                .map(event -> EventMapper.toEventFullDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        confirmedMap.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события ID: {} администратором", eventId);

        Event event = getEventOrThrow(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), true, event.getPublishedOn());
            event.setEventDate(updateRequest.getEventDate());
        }

        CategoryDto categoryDto = null;
        if (updateRequest.getCategory() != null) {
            Category category = getCategoryOrThrow(updateRequest.getCategory());
            categoryDto = CategoryMapper.toCategoryDto(category);
        }

        LocationDto locationDto = updateRequest.getLocation();

        EventMapper.updateFromAdminDto(event, updateRequest, categoryDto, locationDto);
        processStateAction(event, updateRequest.getStateAction(), true);

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие ID: {} обновлено администратором", updatedEvent.getId());

        return enrichEventWithStats(updatedEvent);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from,
                                               Integer size, HttpServletRequest request) {
        log.info("Получение публичных событий с фильтрацией");

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("rangeEnd не может быть раньше rangeStart");
        }

        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(100);
        String searchText = (text != null && !text.isBlank()) ? text.toLowerCase() : null;

        Page<Event> eventsPage = eventRepository.findPublicEvents(start, end, searchText, categories, paid,
                PageRequest.of(from / size, size));

        List<Event> events = new ArrayList<>(eventsPage.getContent());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
            Map<Long, Long> confirmed = getConfirmedRequestsMap(eventIds);

            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());

            if (events.isEmpty()) {
                return Collections.emptyList();
            }
        }

        if ("EVENT_DATE".equals(sort)) {
            events.sort(Comparator.comparing(Event::getEventDate));
        } else if ("VIEWS".equals(sort)) {
            List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
            Map<Long, Long> views = getViewsForEvents(eventIds);
            events.sort((e1, e2) -> views.getOrDefault(e2.getId(), 0L)
                    .compareTo(views.getOrDefault(e1.getId(), 0L)));
        }

        saveHit(request.getRequestURI(), request.getRemoteAddr());
        return mapToEventShortDtoList(events);
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest request) {
        log.info("Получение публичного события по ID: {}", eventId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        saveHit("/events/" + eventId, request.getRemoteAddr());

        return enrichEventWithStats(event);
    }

    private void saveHit(String uri, String ip) {
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hit);
            log.debug("Сохранен просмотр: {}", uri);
        } catch (Exception e) {
            log.error("Ошибка сохранения просмотра для {}: {}", uri, e.getMessage());
        }
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

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {

        return requestRepository
                .countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}