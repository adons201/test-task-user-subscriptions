package ru.tms.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.tms.dto.User;
import ru.tms.entity.UserEntity;
import ru.tms.exceptions.InvalidElementDataException;
import ru.tms.mappers.UserMapper;
import ru.tms.repo.UserRepo;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Реализация сервиса для управления пользователями.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final UserMapper userMapper;

    /**
     * Конструктор класса UserServiceImpl.
     *
     * @param userRepo   Репозиторий для работы с пользователями.
     * @param userMapper Маппер для преобразования между User и UserEntity.
     */
    public UserServiceImpl(UserRepo userRepo, UserMapper userMapper) {
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        log.info("UserService initialized");
    }

    /**
     * Получает пользователя по его ID.
     *
     * @param userId ID пользователя.
     * @return UserEntity Пользователь, найденный по ID.
     * @throws NoSuchElementException Если пользователь с указанным ID не найден.
     */
    @Override
    public UserEntity getUserById(Long userId) {
        log.debug("Fetching user by id {}", userId);
        return this.userRepo.findById(userId)
                .orElseThrow(()-> {
                    String errorMessage = "User not found with id: " + userId;
                    log.error(errorMessage);
                    return new NoSuchElementException(errorMessage);
                });
    }

    /**
     * Получает пользователя по его имени пользователя (username).
     *
     * @param username Имя пользователя (username).
     * @return Optional<UserEntity> Пользователь, найденный по имени пользователя, или Optional.empty(), если пользователь не найден.
     */
    @Override
    public Optional<UserEntity> getUserByUsername(String username) {
        log.debug("Fetching user by username {}", username);
        return this.userRepo.findByUsername(username);
    }

    /**
     * Создает нового пользователя.
     *
     * @param user DTO с данными пользователя.
     * @return User DTO созданного пользователя.
     * @throws InvalidElementDataException Если данные пользователя невалидны (например, пустое имя пользователя).
     * @throws DuplicateKeyException Если пользователь с указанным именем пользователя уже существует.
     */
    @Override
    public User createUser(User user) {
        if (user == null || user.username() == null || user.username().isEmpty()) {
            throw new InvalidElementDataException("Invalid data when trying to create a user: Username is empty or missing");
        } else if (getUserByUsername(user.username()).isPresent()) {
            throw new InvalidElementDataException("User with this username already exists");
        }
        try {
            UserEntity userEntity = new UserEntity(user.username());
            this.userRepo.save(userEntity);
            log.info("Successfully created user with username {}", user.username());
            return this.userMapper.toDto(userEntity);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Username already exists", e);
        }
    }

    /**
     * Обновляет существующего пользователя.
     *
     * @param userId ID пользователя, которого нужно обновить.
     * @param user   DTO с новыми данными пользователя.
     * @return User DTO обновленного пользователя.
     * @throws InvalidElementDataException Если данные пользователя невалидны (например, пустое имя пользователя).
     * @throws DuplicateKeyException Если пользователь с указанным именем пользователя уже существует.
     * @throws OptimisticLockingFailureException Если не удалось обновить пользователя из-за конкурентного изменения.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User updateUser(Long userId, User user) {
        if (user == null || user.username() == null || user.username().isEmpty()) {
            throw new InvalidElementDataException("Invalid data when updating the user: Username is empty or missing");
        } else if (this.getUserByUsername(user.username()).isPresent()) {
            throw new InvalidElementDataException("User with this username already exists");
        }
        try {
            UserEntity userEntity = this.getUserById(userId);
            userEntity.setUsername(user.username());
            this.userRepo.save(userEntity);
            log.info("Updated user with id {}", userId);
            return this.userMapper.toDto(userEntity);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Failed to update user due to concurrent modification", e);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Username already exists", e);
        }
    }

    /**
     * Удаляет пользователя по его ID.
     *
     * @param userId ID пользователя, которого нужно удалить.
     * @throws OptimisticLockingFailureException Если не удалось удалить пользователя из-за конкурентного изменения.
     */
    @Override
    public void deleteUser(Long userId) {
        try {
            UserEntity userEntity = this.getUserById(userId);
            userRepo.delete(userEntity);
            log.info("Deleted user with id {}", userId);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Failed to delete user due to concurrent modification", e);
        }
    }
}
