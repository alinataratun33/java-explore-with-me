package ru.practicum.stats.server;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto endpointHitDto) {
        log.info("Сохранение информации о посещении: URI={}, IP={}, APP={}",
                endpointHitDto.getUri(), endpointHitDto.getIp(), endpointHitDto.getApp());
        EndpointHit endpointHit = statsRepository.save(EndpointHitMapper.toEndpointHit(endpointHitDto));
        log.info("Посещение сохранено с ID={}", endpointHit.getId());
        return EndpointHitMapper.toDto(endpointHit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Запрос статистики: период с {} по {}, URI={}", start, end, uris);

        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        boolean hasUris = uris != null && !uris.isEmpty();
        boolean isUnique = unique != null && unique;

        if (isUnique && hasUris) {
            return statsRepository.findHitsWithUrisWithUniqueIp(start, end, uris);
        } else if (isUnique) {
            return statsRepository.findHitsWithoutUrisWithUniqueIp(start, end);
        } else if (hasUris) {
            return statsRepository.findHitsWithUris(start, end, uris);
        } else {
            return statsRepository.findHitsWithoutUris(start, end);
        }
    }
}

