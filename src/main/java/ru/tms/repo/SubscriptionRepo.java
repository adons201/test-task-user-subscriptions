package ru.tms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tms.entity.SubscriptionEntity;
import ru.tms.entity.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepo extends JpaRepository<SubscriptionEntity,  Long> {

    Optional<SubscriptionEntity> findByNameAndUser(String name, UserEntity userEntity);

    List<SubscriptionEntity> findByUserId(Long userId);

    void deleteByUserIdAndId(Long userId, Long Id);

    @Query(value = "SELECT * " +
                " FROM (SELECT name FROM user_subscriptions.subscriptions GROUP BY name ORDER BY COUNT(*) DESC LIMIT 3) as sn" +
                " ORDER BY name"
                , nativeQuery = true)
    List<String> findTopThreeSubscriptions();
}
