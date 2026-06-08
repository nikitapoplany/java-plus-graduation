package ru.practicum.internal.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.web.user.dto.UserDto;

@FeignClient(name = "USER-SERVICE", path = "/internal/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable Long userId);

    @GetMapping("/{userId}/exists")
    boolean exists(@PathVariable Long userId);
}
