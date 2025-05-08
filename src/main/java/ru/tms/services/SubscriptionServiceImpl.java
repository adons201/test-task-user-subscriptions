package ru.tms.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.View;
import ru.tms.dto.Subscription;
import ru.tms.entity.SubscriptionEntity;
import ru.tms.entity.UserEntity;
import ru.tms.exceptions.InvalidElementDataException;
import ru.tms.mappers.SubscriptionMapper;
import ru.tms.repo.SubscriptionRepo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Реализация сервиса для управления подписками.
 */
@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepo subscriptionRepo;
    private final SubscriptionMapper subscriptionMapper;
    private final UserServiceImpl userServiceImpl;

    /**
     * Конструктор класса SubscriptionServiceImpl.
     *
     * @param subscriptionRepo Репозиторий для работы с подписками.
     * @param subscriptionMapper Маппер для преобразования между Subscription и SubscriptionEntity.
     * @param userServiceImpl Сервис для работы с пользователями.
     */
    public SubscriptionServiceImpl(SubscriptionRepo subscriptionRepo, SubscriptionMapper subscriptionMapper, UserServiceImpl userServiceImpl, View error) {
        this.subscriptionRepo = subscriptionRepo;
        this.subscriptionMapper = subscriptionMapper;
        this.userServiceImpl = userServiceImpl;
        log.info("SubscriptionServiceImpl initialized");
    }

    /**
     * Получает подписку по ее ID.
     *
     * @param subscriptionId ID подписки.
     * @return SubscriptionEntity Подписка, найденная по ID.
     * @throws NoSuchElementException Если подписка с указанным ID не найдена.
     */
    @Override
    public SubscriptionEntity getSubscriptionById(Long subscriptionId) {
        log.debug("Fetching subscription by id {}", subscriptionId);
        return this.subscriptionRepo.findById(subscriptionId)
                .orElseThrow(()->new NoSuchElementException("Subscription with id " + subscriptionId + " not found."));
    }

    /**
     * Получает подписку по имени и пользователю.
     *
     * @param name Имя подписки.
     * @param userEntity Пользователь, которому принадлежит подписка.
     * @return Optional<SubscriptionEntity> Подписка, найденная по имени и пользователю, или Optional.empty(), если подписка не найдена.
     */
    @Override
    public Optional<SubscriptionEntity> getSubscriptionByUserIdAndName(String name, UserEntity userEntity) {
        log.debug("Searching for subscription named {} under user {}", name, userEntity.getUsername());
        return this.subscriptionRepo.findByNameAndUser(name, userEntity);
    }

    /**
     * Получает список подписок по ID пользователя.
     *
     * @param userId ID пользователя.
     * @return List<Subscription> Список подписок пользователя.
     */
    @Override
    public List<Subscription> getSubscriptionsByUserId(Long userId) {
        log.debug("Fetching subscriptions by userId {}", userId);
        List<SubscriptionEntity> subscriptionEntities = this.subscriptionRepo.findByUserId(userId);
        return subscriptionMapper.toDto(subscriptionEntities);
    }

    /**
     * Получает список трех самых популярных подписок.
     *
     * @return List<String> Список имен трех самых популярных подписок.
     */
    @Override
    public List<String> findTopThreeSubscriptions() {
        log.debug("Fetching top three subscription");
        return subscriptionRepo.findTopThreeSubscriptions();
    }

    /**
     * Создает новую подписку для пользователя.
     *
     * @param subscription DTO с данными подписки.
     * @param userId ID пользователя, для которого создается подписка.
     * @return Subscription DTO созданной подписки.
     * @throws InvalidElementDataException Если данные подписки невалидны.
     * @throws DuplicateKeyException Если подписка с указанным именем уже существует для данного пользователя.
     */
    @Override
    public Subscription createSubscription(Subscription subscription, Long userId) {
        UserEntity userEntity = userServiceImpl.getUserById(userId);
        if (subscription == null || subscription.name() == null || subscription.name().isEmpty()) {
            throw new InvalidElementDataException("Subscription.name cannot be empty or missing");
        } else if (getSubscriptionByUserIdAndName(subscription.name(), userEntity).isPresent()) {
            throw new InvalidElementDataException(
                    String.format("Attempted duplicate subscription creation for user %s: %s",
                            userEntity.getUsername(), subscription.name()));
        }
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity(subscription.name(), userEntity);
        try {
            subscriptionRepo.save(subscriptionEntity);
            log.info("Created new subscription with name {}", subscription.name());
            return subscriptionMapper.toDto(subscriptionEntity);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Subscription already exists", e);
        }
    }

    /**
     * Удаляет подписку пользователя.
     *
     * @param userId ID пользователя.
     * @param subscriptionId ID подписки.
     * @throws OptimisticLockingFailureException Если не удалось удалить подписку из-за конкурентного изменения.
     */
    @Override
    @Transactional
    public void deleteSubscription(Long userId, Long subscriptionId) {
        try {
            SubscriptionEntity subscriptionEntity = this.getSubscriptionById(subscriptionId);
            subscriptionRepo.deleteByUserIdAndId(userId, subscriptionEntity.getId());
            log.info("Deleted subscription with userId {} and subscriptionId {}", userId, subscriptionId);
        } catch (OptimisticLockingFailureException e) {
            throw new OptimisticLockingFailureException("Failed to delete subscription due to concurrent modification"
                    , e);
        }
    }
}
