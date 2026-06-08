package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.dto.UserDto;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.user.mapper.UserMapper;
import ru.practicum.web.user.repository.UserRepository;
import ru.practicum.web.user.validator.UserValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final UserValidator validator;

    @Override
    public UserDto create(NewUserRequest request) {
        log.info("Creating user: email={}, name='{}'", request.getEmail(), request.getName());
        validator.validateCreateRequest(request);

        try {
            User saved = userRepository.save(UserMapper.fromNewUserRequest(request));
            return UserMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            validator.checkEmailUnique(request.getEmail());
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }
    }

    @Override
    public void delete(Long userId) {
        validator.validateUserExists(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        validator.validatePagination(from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        if (ids != null && !ids.isEmpty()) {
            return userRepository.findByIdIn(ids, pageable)
                    .stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
        }

        return userRepository.findAll(pageable)
                .stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }
}
