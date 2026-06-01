package ru.practicum.main.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.users.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));
    }

    @Override
    public UserDto getUserById(Long id) {
        log.info("Поиск пользователя по ID: {}", id);

        User user = getUserOrThrow(id);
        log.info("Пользователь найден: {}", user.getName());
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Создание нового пользователя с email: {}", userDto.getEmail());

        checkEmailUniqueness(userDto.getEmail());

        User user = UserMapper.toUser(userDto);
        User savedUser = userRepository.save(user);

        log.info("Пользователь успешно создан с ID: {}", savedUser.getId());
        return UserMapper.toDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение пользователей: ids={}, from={}, size={}", ids, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<User> users;

        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findByIdIn(ids, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Удаление пользователя с ID: {}", id);
        getUserOrThrow(id);

        userRepository.deleteById(id);
    }

    private void checkEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Попытка создать/обновить пользователя с уже существующим email: {}", email);
            throw new ConflictException("Email уже существует: " + email);
        }
    }
}
