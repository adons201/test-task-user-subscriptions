package ru.tms.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import ru.tms.dto.User;
import ru.tms.entity.UserEntity;
import ru.tms.exceptions.InvalidElementDataException;
import ru.tms.mappers.UserMapper;
import ru.tms.repo.UserRepo;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity userEntity;
    private User user;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity("testUser");
        userEntity.setId(1L);
        user = User.builder().username("testUser").build();
    }

    @Test
    @DisplayName("Должен корректно вернуть UserEntity по id")
    void getUserById_ExistingId_ReturnsUserEntity() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));

        // Act
        UserEntity result = userService.getUserById(1L);

        // Assert
        assertThat(result).isEqualTo(userEntity);
        verify(userRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть исключение при поиске UserEntity по id")
    void getUserById_NonExistingId_ThrowsNoSuchElementException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("User not found with id: 1");
        verify(userRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть UserEntity по username")
    void getUserByUsername_ExistingUsername_ReturnsOptionalWithUserEntity() {
        // Arrange
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.of(userEntity));

        // Act
        Optional<UserEntity> result = userService.getUserByUsername("testUser");

        // Assert
        assertThat(result).isPresent().contains(userEntity);
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно не найти UserEntity по username")
    void getUserByUsername_NonExistingUsername_ReturnsEmptyOptional() {
        // Arrange
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());

        // Act
        Optional<UserEntity> result = userService.getUserByUsername("testUser");

        // Assert
        assertThat(result).isEmpty();
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно создать UserEntity")
    void createUser_ValidUser_ReturnsCreatedUser() {
        // Arrange
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(user);

        // Act
        User result = userService.createUser(user);

        // Assert
        assertThat(result).isEqualTo(user);
        verify(userRepo, times(1)).save(any(UserEntity.class));
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class при создании UserEntity")
    void createUser_NullUser_ThrowsInvalidElementDataException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(null))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Invalid data when trying to create a user: Username is empty or missing");
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class при создании UserEntity")
    void createUser_NullUsername_ThrowsInvalidElementDataException() {
        // Arrange
        User invalidUser = User.builder().build();

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(invalidUser))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Invalid data when trying to create a user: Username is empty or missing");

    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class при создании UserEntity")
    void createUser_EmptyUsername_ThrowsInvalidElementDataException() {
        // Arrange
        User invalidUser = User.builder().username("").build();

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(invalidUser))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Invalid data when trying to create a user: Username is empty or missing");
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class при создании UserEntity")
    void createUser_DuplicateUsername_ThrowsInvalidElementDataException() {
        // Arrange
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.of(userEntity));

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("User with this username already exists");
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть DuplicateKeyException.class при создании UserEntity")
    void createUser_DuplicateKeyExceptionFromRepo_ThrowsDuplicateKeyException() {
        // Arrange
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenThrow(new DuplicateKeyException("Username already exists"));

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("Username already exists");
        verify(userRepo, times(1)).save(any(UserEntity.class));
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно обновить UserEntity")
    void updateUser_ValidUser_ReturnsUpdatedUser() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userMapper.toDto(userEntity)).thenReturn(user);

        // Act
        User result = userService.updateUser(1L, user);

        // Act & Assert
        assertThat(result).isEqualTo(user);
        verify(userRepo, times(1)).save(any(UserEntity.class));
        verify(userRepo, times(1)).findById(any(Long.class));
        verify(userRepo, times(1)).findByUsername(any(String.class));
    }

    @Test
    @DisplayName("Должен корректно не найти User по id")
    void updateUser_NonExistingUser_ThrowsNoSuchElementException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, user))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("User not found with id: 1");
        verify(userRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class, User пришел пустой")
    void updateUser_NullUser_ThrowsInvalidElementDataException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, null))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Invalid data when updating the user: Username is empty or missing");
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class, поля Users пустые")
    void updateUser_NullUsername_ThrowsInvalidElementDataException() {
        // Arrange
        User invalidUser = User.builder().build();

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, invalidUser))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Invalid data when updating the user: Username is empty or missing");
    }

    @Test
    @DisplayName("Должен корректно вернуть InvalidElementDataException.class, такой username уже существует")
    void updateUser_DuplicateUsername_ThrowsInvalidElementDataException() {
        // Arrange
        UserEntity anotherUser = new UserEntity("testUser");
        anotherUser.setId(2L);
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.of(anotherUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, user))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("User with this username already exists");
        verify(userRepo, times(1)).findByUsername("testUser");
    }

    @Test
    @DisplayName("Должен корректно вернуть OptimisticLockingFailureException.class при обновлении")
    void updateUser_OptimisticLockingFailureException_ThrowsIllegalStateException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenThrow(new OptimisticLockingFailureException("Concurrent modification"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to update user due to concurrent modification");
        verify(userRepo, times(1)).findById(any(Long.class));
        verify(userRepo, times(1)).findByUsername(any(String.class));
        verify(userRepo, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть DuplicateKeyException.class при обновлении")
    void updateUser_DuplicateKeyExceptionFromRepo_ThrowsDuplicateKeyException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepo.findByUsername("testUser")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenThrow(new DuplicateKeyException("Username already exists"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, user))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("Username already exists");
        verify(userRepo, times(1)).findById(any(Long.class));
        verify(userRepo, times(1)).findByUsername(any(String.class));
        verify(userRepo, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Должен корректно удалить удалить user")
    void deleteUser_ExistingUser_DeletesUser() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepo, times(1)).delete(userEntity);
        verify(userRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть NoSuchElementException.class при удалении user")
    void deleteUser_NonExistingUser_ThrowsNoSuchElementException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    @DisplayName("Должен корректно вернуть OptimisticLockingFailureException.class при удалении")
    void deleteUser_OptimisticLockingFailureException_ThrowsIllegalStateException() {
        // Arrange
        when(userRepo.findById(1L)).thenReturn(Optional.of(userEntity));
        doThrow(new OptimisticLockingFailureException("Concurrent modification")).when(userRepo).delete(userEntity);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to delete user due to concurrent modification");
        verify(userRepo, times(1)).findById(any(Long.class));
    }
}