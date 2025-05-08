package ru.tms.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import ru.tms.dto.Subscription;
import ru.tms.entity.SubscriptionEntity;
import ru.tms.entity.UserEntity;
import ru.tms.exceptions.InvalidElementDataException;
import ru.tms.mappers.SubscriptionMapper;
import ru.tms.repo.SubscriptionRepo;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepo subscriptionRepo;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private UserServiceImpl userServiceImpl;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UserEntity userEntity;
    private Subscription subscription;
    private SubscriptionEntity subscriptionEntity;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUsername("testUser");

        subscription = Subscription.builder().name("TestSubscription").build();

        subscriptionEntity = new SubscriptionEntity("TestSubscription", userEntity);
        subscriptionEntity.setId(1L);
    }

    @Test
    @DisplayName("Должен корректно вернуть подписку по Id")
    void getSubscriptionById_ExistingId_ReturnsSubscriptionEntity() {
        // Arrange
        when(subscriptionRepo.findById(1L)).thenReturn(Optional.of(subscriptionEntity));

        // Act
        SubscriptionEntity result = subscriptionService.getSubscriptionById(1L);

        //Assert
        assertThat(result).isEqualTo(subscriptionEntity);
        verify(subscriptionRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть пустой результат запроса подписки по Id")
    void getSubscriptionById_NonExistingId_ThrowsNoSuchElementException() {
        // Arrange
        when(subscriptionRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Subscription with id 1 not found.");
        verify(subscriptionRepo, times(1)).findById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть подписку по полям name, UserEntity")
    void getSubscriptionByUserIdAndName_ExistingSubscription_ReturnsSubscriptionEntity() {
        // Arrange
        when(subscriptionRepo.findByNameAndUser("TestSubscription", userEntity))
                .thenReturn(Optional.of(subscriptionEntity));

        // Act
        Optional<SubscriptionEntity> result = subscriptionService
                .getSubscriptionByUserIdAndName("TestSubscription", userEntity);

        // Assert
        assertThat(result).isPresent().contains(subscriptionEntity);
        verify(subscriptionRepo, times(1)).findByNameAndUser(any(String.class), any(UserEntity.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть пустой результат запроса подписки по полям name, UserEntity")
    void getSubscriptionByUserIdAndName_NonExistingSubscription_ReturnsEmptyOptional() {
        // Arrange
        when(subscriptionRepo.findByNameAndUser("TestSubscription", userEntity)).thenReturn(Optional.empty());

        // Act
        Optional<SubscriptionEntity> result = subscriptionService
                .getSubscriptionByUserIdAndName("TestSubscription", userEntity);

        // Assert
        assertThat(result).isEmpty();
        verify(subscriptionRepo, times(1)).findByNameAndUser(any(String.class), any(UserEntity.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть список подписок пользователя")
    void getSubscriptionsByUserId_ExistingUserId_ReturnsListOfSubscriptions() {
        // Arrange
        List<SubscriptionEntity> subscriptionEntities = Collections.singletonList(subscriptionEntity);
        when(subscriptionRepo.findByUserId(1L)).thenReturn(subscriptionEntities);
        when(subscriptionMapper.toDto(subscriptionEntities)).thenReturn(Collections.singletonList(subscription));

        // Act
        List<Subscription> result = subscriptionService.getSubscriptionsByUserId(1L);

        // Assert
        assertThat(result).isNotEmpty().contains(subscription);
        verify(subscriptionRepo, times(1)).findByUserId(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно вернуть список топ 3 подписки")
    void findTopThreeSubscriptions_ReturnsListOfSubscriptionNames() {
        // Arrange
        List<String> topSubscriptions = Arrays.asList("Sub1", "Sub2", "Sub3");
        when(subscriptionRepo.findTopThreeSubscriptions()).thenReturn(topSubscriptions);

        // Act
        List<String> result = subscriptionService.findTopThreeSubscriptions();

        // Assert
        assertThat(result).isEqualTo(topSubscriptions);
    }

    @Test
    @DisplayName("Должен корректно создать подписку")
    void createSubscription_ValidSubscription_ReturnsCreatedSubscription() {
        // Arrange
        when(userServiceImpl.getUserById(1L)).thenReturn(userEntity);
        when(subscriptionRepo.findByNameAndUser("TestSubscription", userEntity))
                .thenReturn(Optional.empty());
        when(subscriptionMapper.toDto(any(SubscriptionEntity.class))).thenReturn(subscription);

        // Act
        Subscription result = subscriptionService.createSubscription(subscription, 1L);

        // Assert
        assertThat(result).isEqualTo(subscription);
        verify(userServiceImpl, times(1)).getUserById(any(Long.class));
        verify(subscriptionRepo, times(1)).findByNameAndUser(any(String.class), any(UserEntity.class));
        verify(subscriptionRepo, times(1)).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Должен корректно не создать подписку, из-за пустых полей Subscription")
    void createSubscription_NullSubscriptionName_ThrowsInvalidElementDataException() {
        // Arrange
        Subscription invalidSubscription = Subscription.builder().build();
        when(userServiceImpl.getUserById(1L)).thenReturn(userEntity);

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.createSubscription(invalidSubscription, 1L))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage("Subscription.name cannot be empty or missing");
        verify(userServiceImpl, times(1)).getUserById(any(Long.class));
    }

    @Test
    @DisplayName("Должен корректно не создать подписку, из-за наличия такой же подписки на пользователе")
    void createSubscription_DuplicateSubscription_ThrowsInvalidElementDataException() {
        // Arrange
        when(userServiceImpl.getUserById(1L)).thenReturn(userEntity);
        when(subscriptionRepo.findByNameAndUser("TestSubscription", userEntity))
                .thenReturn(Optional.of(subscriptionEntity));

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.createSubscription(subscription, 1L))
                .isInstanceOf(InvalidElementDataException.class)
                .hasMessage(String.format("Attempted duplicate subscription creation for user %s: %s",
                        userEntity.getUsername(), subscription.name()));
        verify(userServiceImpl, times(1)).getUserById(any(Long.class));
        verify(subscriptionRepo, times(1)).findByNameAndUser(any(String.class), any(UserEntity.class));
    }

    @Test
    @DisplayName("Должен корректно удалить подписку")
    void deleteSubscription_ExistingSubscription_DeletesSubscription() {
        // Arrange
        when(subscriptionRepo.findById(1L)).thenReturn(Optional.of(subscriptionEntity));

        // Act
        subscriptionService.deleteSubscription(1L, 1L);

        // Assert
        verify(subscriptionRepo, times(1)).deleteByUserIdAndId(1L, 1L);
        verify(subscriptionRepo, times(1)).findById(any(Long.class));
    }
}
