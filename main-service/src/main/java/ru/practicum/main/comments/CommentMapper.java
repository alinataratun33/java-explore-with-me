package ru.practicum.main.comments;

import ru.practicum.main.comments.dto.CommentDto;
import ru.practicum.main.comments.dto.NewCommentDto;
import ru.practicum.main.events.Event;
import ru.practicum.main.users.User;

import java.time.LocalDateTime;

public class CommentMapper {

    public static CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setDescription(comment.getDescription());
        dto.setCreated(comment.getCreated());
        dto.setAuthorName(comment.getAuthor().getName());
        dto.setUpdated(comment.getUpdated());
        return dto;
    }

    public static Comment toComment(NewCommentDto dto, User author, Event event) {
        Comment comment = new Comment();
        comment.setDescription(dto.getDescription());
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now());
        comment.setUpdated(null);
        return comment;
    }
}
