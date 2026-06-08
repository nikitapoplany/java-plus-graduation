package ru.practicum.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.dto.UserDto;
import ru.practicum.web.validation.ValidationConstants;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody @Valid NewUserRequest request) {
        UserDto created = userAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_FROM + "") int from,
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_SIZE + "") int size
    ) {
        if (from < ValidationConstants.PAGE_MIN_FROM) {
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size < ValidationConstants.PAGE_MIN_SIZE) {
            throw new BadRequestException("Parameter 'size' must be positive");
        }
        return ResponseEntity.ok(userAdminService.getUsers(ids, from, size));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        userAdminService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
