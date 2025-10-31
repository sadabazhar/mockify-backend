package com.mockify.backend.mapper;

import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Entity -> Response
    UserResponse toResponse(User user);
    List<UserResponse> toResponseList(List<User> users);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    User toEntity(RegisterRequest request);
}


