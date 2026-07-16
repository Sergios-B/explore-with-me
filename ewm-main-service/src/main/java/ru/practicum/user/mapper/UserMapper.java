package ru.practicum.user.mapper;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;

public class UserMapper {

    public static User toUser(NewUserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .build();
    }

    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static ru.practicum.user.dto.UserShortDto toUserShortDto(User user) {
        return ru.practicum.user.dto.UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}