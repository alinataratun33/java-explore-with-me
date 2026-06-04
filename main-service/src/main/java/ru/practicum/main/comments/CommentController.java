package ru.practicum.main.comments;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.comments.dto.CommentDto;
import ru.practicum.main.comments.dto.NewCommentDto;
import ru.practicum.main.comments.dto.UpdateCommentDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{userId}/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId, @PathVariable Long eventId,
                                    @RequestBody @Valid NewCommentDto newCommentDto) {
        log.info("Запрос на создание комментария пользователем {} к событию {}", userId, eventId);
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getCommentsByEvent(@PathVariable Long eventId,
                                               @RequestParam(defaultValue = "0") Integer from,
                                               @RequestParam(defaultValue = "10") Integer size) {
        log.info("Запрос на получение комментариев к событию {}", eventId);
        return commentService.getCommentsByEvent(eventId, from, size);
    }

    @PatchMapping("/{userId}/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody @Valid UpdateCommentDto updateCommentDto) {
        log.info("Запрос на редактирование комментария c ID {}", commentId);
        return commentService.updateComment(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/user/{userId}/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByUser(@PathVariable Long userId, @PathVariable Long commentId) {
        log.info("Запрос на удаление комментария с ID: {}", commentId);
        commentService.deleteCommentByUser(userId, commentId);
    }

    @DeleteMapping("/admin/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long commentId) {
        log.info("Запрос на удаление комментария c ID: {} администратором", commentId);
        commentService.deleteCommentByAdmin(commentId);
    }
}
