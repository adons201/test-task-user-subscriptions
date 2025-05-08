package ru.tms.mappers;

import org.mapstruct.Mapper;
import ru.tms.dto.User;
import ru.tms.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDto(UserEntity userEntity);
    UserEntity toEntity(User user);
}
