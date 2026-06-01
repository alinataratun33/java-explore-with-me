package ru.practicum.main.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.events.Event;
import ru.practicum.main.events.EventRepository;
import ru.practicum.main.events.enums.State;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.requests.dto.RequestDto;
import ru.practicum.main.users.User;
import ru.practicum.main.users.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Мероприятие не найдено с ID: " + id));
    }

    private Request getRequestOrThrow(Long requestId, Long userId) {
        return requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена или не принадлежит пользователю"));
    }

    @Override
    public List<RequestDto> getUserRequests(Long userId) {
        log.debug("Получение всех заявок пользователя с ID: {}", userId);

        getUserOrThrow(userId);
        List<Request> requests = requestRepository.findByRequesterId(userId);
        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание заявки: userId={}, eventId={}", userId, eventId);

        User user = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Повторный запрос на участие не разрешён");
        }

        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит запросов на участие");
            }
        }

        Request request = RequestMapper.toRequest(event, user);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);

        log.info("Заявка создана с ID: {}, статус: {}", savedRequest.getId(), savedRequest.getStatus());
        return RequestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки на участие в событии");
        getUserOrThrow(userId);
        Request request = getRequestOrThrow(requestId, userId);

        request.setStatus(RequestStatus.CANCELED);
        Request savedRequest = requestRepository.save(request);

        log.info("Заявка с ID: {} отменена", requestId);
        return RequestMapper.toDto(savedRequest);
    }

    @Override
    public List<RequestDto> getUserRequestByEvent(Long userId, Long eventId) {
        log.info("Получение всех заявок на событие ID: {} для пользователя ID: {}", eventId, userId);

        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор события может просматривать заявки");
        }

        List<Request> requests = requestRepository.findByEventId(eventId);

        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статуса заявки с ID: {}", eventId);
        Event event = getEventOrThrow(eventId);
        getUserOrThrow(userId);

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Для этого события не требуется подтверждение заявок");
        }

        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит");
        }

        List<Request> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        for (Request r : requests) {
            if (!r.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка не относится к этому событию");
            }
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Заявка не в статусе ожидания");
            }
        }

        List<RequestDto> confirmedList = new ArrayList<>();
        List<RequestDto> rejectedList = new ArrayList<>();

        long availableSlots = event.getParticipantLimit() - confirmed;
        int confirmedCount = 0;

        boolean isConfirm = updateRequest.getStatus() == RequestStatus.CONFIRMED;

        for (Request r : requests) {
            if (isConfirm && confirmedCount < availableSlots) {
                r.setStatus(RequestStatus.CONFIRMED);
                confirmedList.add(RequestMapper.toDto(r));
                confirmedCount++;
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejectedList.add(RequestMapper.toDto(r));
            }
        }

        requestRepository.saveAll(requests);
        return new EventRequestStatusUpdateResult(confirmedList, rejectedList);
    }
}
