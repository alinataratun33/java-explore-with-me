package ru.practicum.main.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserShortDto {
    private Long id;
    @NotBlank
    @Size(min = 2, max = 250)
    private String name;
}
