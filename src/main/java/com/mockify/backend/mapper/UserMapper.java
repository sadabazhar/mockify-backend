package com.mockify.backend.mapper;

import com.mockify.backend.model.User;
import com.mockify.backend.dto.UserDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Converts User entity to UserDTO for sending in API responses.
    UserDTO toDto(User user);

    // Converts List of User entity to list of UserDTO
    List<UserDTO> toDtoList(List<User> users);

    // Converts UserDTO to User entity
    User toEntity(UserDTO dto);

    // Converts List of UserDTO to list of User entity
    List<User> toEntityList(List<UserDTO> dtos);

    /*
        Updates an existing User entity with non-null values from a DTO.
        Ignores null values so existing data is not overwritten.
        Does not touch id or createdAt to maintain data integrity.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateUserFromDto(UserDTO dto, @MappingTarget User entity);

}

