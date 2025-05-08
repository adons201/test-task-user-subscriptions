package ru.tms.services;

import ru.tms.dto.User;
import ru.tms.entity.UserEntity;

import java.util.Optional;

public interface UserService {

    UserEntity getUserById(Long userId);

    Optional<UserEntity> getUserByUsername(String username);

    User createUser(User user);

    User updateUser(Long userId, User user);

    void deleteUser(Long userId);
}
