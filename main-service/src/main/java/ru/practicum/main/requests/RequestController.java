package ru.practicum.main.requests;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.requests.dto.RequestDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService requestService;

    @GetMapping("/requests")
    public List<RequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("Запрос на получение заявок пользователя с ID: {}", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        log.info("Запрос на создание заявки пользователем: {}, на участие в событии: {}", userId, eventId);
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public RequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("Запрос на удаление заявки с ID: {}", requestId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<RequestDto> getUserRequestByEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Запрос на получение информации о запросах пользователя: {}, на участие в событии: {}", userId, eventId);
        return requestService.getUserRequestByEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                              @RequestBody @Valid EventRequestStatusUpdateRequest updateRequest) {
        log.info("Запрос на изменение статуса заявки пользователя: {}, в событии: {}", userId, eventId);
        return requestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
