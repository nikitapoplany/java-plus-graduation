package ru.practicum.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.user.dto.UserDto;
import ru.practicum.web.user.mapper.UserMapper;
import ru.practicum.web.user.repository.UserRepository;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    @GetMapping("/{userId}/exists")
    public boolean exists(@PathVariable Long userId) {
        return userRepository.existsById(userId);
    }
}
