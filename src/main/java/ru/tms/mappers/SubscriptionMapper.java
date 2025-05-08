package ru.tms.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tms.dto.Subscription;
import ru.tms.entity.SubscriptionEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(target = "user", expression = "java(subscriptionEntity.getUser().getId())")
    Subscription toDto(SubscriptionEntity subscriptionEntity);
    List<Subscription> toDto(List<SubscriptionEntity> subscriptionEntities);
}
