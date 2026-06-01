package ru.practicum.main.users;

import ru.practicum.main.users.dto.UserDto;
import ru.practicum.main.users.dto.UserShortDto;

public class UserMapper {

    public static UserDto toDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    public static UserShortDto toShortDto(User user) {
        UserShortDto userShortDro = new UserShortDto();
        userShortDro.setId(user.getId());
        userShortDro.setName(user.getName());
        return userShortDro;
    }

    public static User toUser(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        return user;
    }
}