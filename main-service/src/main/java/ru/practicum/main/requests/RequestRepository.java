package ru.practicum.main.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query("SELECT r.event.id, COUNT(r) " +
            "FROM Request r " +
            "WHERE r.event.id IN :ids AND r.status = :status " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("ids") List<Long> ids,
                                                    @Param("status") RequestStatus status);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long userId);

    List<Request> findByEventId(Long eventId);
}