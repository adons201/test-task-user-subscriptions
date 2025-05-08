package ru.tms.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tms.dto.User;
import ru.tms.entity.UserEntity;
import ru.tms.mappers.UserMapper;
import ru.tms.services.UserService;
import ru.tms.services.UserServiceImpl;

@Slf4j
@RestController
@RequestMapping("/user-subscriptions/v1")
@Tag(name = "User", description = "the User in service user-subscriptions")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserServiceImpl userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        log.info("UserController initialized");
    }

    @Operation(summary = "Get user by id", description = "Return user by userId", tags = {"User"})
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long userId) {
        log.debug("Received request to fetch user by id {}", userId);
        UserEntity userEntity = this.userService.getUserById(userId);
        return new ResponseEntity<>(this.userMapper.toDto(userEntity), HttpStatus.OK);
    }

    @Operation(summary = "Create user", description = "Return create user", tags = {"User"})
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody @Valid User user) {
        log.debug("Creating new user: {}", user);
        User createUser = this.userService.createUser(user);
        log.debug("Successfully create user with id {}", createUser.id());
        return new ResponseEntity<>(createUser, HttpStatus.OK);
    }

    @Operation(summary = "Update user", description = "Return update user", tags = {"User"})
    @PutMapping(value ="/users/{id}", consumes = "application/json")
    public ResponseEntity<User> updateUser(@PathVariable("id") Long userId, @RequestBody @Valid User user) {
        log.debug("Updating user with id {}", userId);
        return new ResponseEntity<>(this.userService.updateUser(userId, user), HttpStatus.OK);
    }

    @Operation(summary = "Delete user", description = "Return delete user", tags = {"User"})
    @DeleteMapping("/users/{id}")
    public ResponseEntity<User> deleteUser(@PathVariable("id") Long userId) {
        log.debug("Deleting user with id {}", userId);
        this.userService.deleteUser(userId);
        log.debug("Successfully deleted user with ID {}", userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
