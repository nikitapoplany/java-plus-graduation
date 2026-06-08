package ru.practicum.user;

import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.dto.UserDto;

import java.util.List;

public interface UserAdminService {
    UserDto create(NewUserRequest request);

    void delete(Long userId);

    List<UserDto> getUsers(List<Long> ids, int from, int size);
}
