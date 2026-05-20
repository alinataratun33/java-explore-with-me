package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class StatsClient {
    private final RestTemplate rest;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-server.url:http://localhost:9090}") String serverUrl,
                       RestTemplateBuilder builder) {
        this.rest = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .build();
    }

    public ResponseEntity<Object> saveHit(EndpointHitDto hit) {
        return rest.postForEntity("/hit", hit, Object.class);
    }

    public ResponseEntity<Object> getStats(LocalDateTime start, LocalDateTime end,
                                           List<String> uris, Boolean unique) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", start.format(FORMATTER));
        params.put("end", end.format(FORMATTER));
        params.put("unique", unique != null && unique);

        StringBuilder path = new StringBuilder("/stats?start={start}&end={end}&unique={unique}");

        if (uris != null && !uris.isEmpty()) {
            for (int i = 0; i < uris.size(); i++) {
                path.append("&uris={uris").append(i).append("}");
                params.put("uris" + i, uris.get(i));
            }
        }

        return rest.getForEntity(path.toString(), Object.class, params);
    }
}