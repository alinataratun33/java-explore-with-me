package ru.practicum.main.requests;

import ru.practicum.main.events.Event;
import ru.practicum.main.requests.dto.RequestDto;
import ru.practicum.main.users.User;

import java.time.LocalDateTime;

public class RequestMapper {
    public static RequestDto toDto(Request request) {

        RequestDto dto = new RequestDto();
        dto.setId(request.getId());
        dto.setRequester(request.getRequester().getId());
        dto.setEvent(request.getEvent().getId());
        dto.setStatus(request.getStatus());
        dto.setCreated(request.getCreated());
        return dto;
    }

    public static Request toRequest(Event event, User requester) {
        Request request = new Request();
        request.setRequester(requester);
        request.setEvent(event);
        request.setStatus(RequestStatus.PENDING);
        request.setCreated(LocalDateTime.now());
        return request;
    }
}