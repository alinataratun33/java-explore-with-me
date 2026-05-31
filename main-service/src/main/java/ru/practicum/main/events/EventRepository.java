package ru.practicum.main.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.events.enums.State;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByCategoryId(Long categoryId);

    Page<Event> findByInitiatorId(Long initiatorId, Pageable pageable);


    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH e.category " +
            "WHERE (COALESCE(:users, NULL) IS NULL OR e.initiator.id IN :users) " +
            "AND (COALESCE(:states, NULL) IS NULL OR e.state IN :states) " +
            "AND (COALESCE(:categories, NULL) IS NULL OR e.category.id IN :categories) " +
            "AND (COALESCE(:startDate, NULL) IS NULL OR e.eventDate >= :startDate) " +
            "AND (COALESCE(:endDate, NULL) IS NULL OR e.eventDate <= :endDate)")
    Page<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<State> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH e.category " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd " +
            "AND (:text IS NULL OR " +
            "     LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%')) OR " +
            "     LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) ")
    Page<Event> findPublicEvents(@Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 Pageable pageable);
}