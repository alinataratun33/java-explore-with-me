package ru.practicum.main.comments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.comments.dto.CommentDto;
import ru.practicum.main.comments.dto.NewCommentDto;
import ru.practicum.main.comments.dto.UpdateCommentDto;
import ru.practicum.main.events.Event;
import ru.practicum.main.events.EventRepository;
import ru.practicum.main.events.enums.State;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.users.User;
import ru.practicum.main.users.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
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

    private Comment getCommentOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден с ID: " + id));
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);
        User author = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != State.PUBLISHED) {
            throw new ValidationException("Невозможно оставить комментарий к неопубликованному событию");
        }

        Comment comment = CommentMapper.toComment(newCommentDto, author, event);
        Comment savedComment = commentRepository.save(comment);

        log.info("Комментарий создан");
        return CommentMapper.toDto(savedComment);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, Integer from, Integer size) {
        log.info("Получение комментариев для события с ID: {}", eventId);

        getEventOrThrow(eventId);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> commentsPage = commentRepository.findByEventIdOrderByCreatedDesc(eventId, pageable);

        return commentsPage.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.info("Редактирование комментария с ID: {}", commentId);

        Comment comment = getCommentOrThrow(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Комментарий может редактировать только автор");
        }

        comment.setDescription(updateCommentDto.getDescription());
        comment.setUpdated(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Удаление комментария пользователем с ID: {}", commentId);
        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Комментарий может удалить только автор");
        }

        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Удаление комментария администратором с ID: {}", commentId);

        getCommentOrThrow(commentId);
        commentRepository.deleteById(commentId);
    }
}