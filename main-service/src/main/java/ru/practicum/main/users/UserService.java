package ru.practicum.main.users;

import ru.practicum.main.users.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto getUserById(Long id);

    UserDto createUser(UserDto userDto);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long id);
}