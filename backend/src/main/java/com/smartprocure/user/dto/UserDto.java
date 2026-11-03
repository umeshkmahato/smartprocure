package com.smartprocure.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public user representation — never exposes {@code passwordHash}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
        Long          id,
        String        username,
        String        email,
        String        firstName,
        String        lastName,
        String        fullName,
        String        department,
        String        phone,
        UserStatus    status,
        Set<String>   roles,
        LocalDateTime createdAt
) {
    /** Convert a {@link User} entity to a transport-safe {@link UserDto}. */
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getDepartment(),
                user.getPhone(),
                user.getStatus(),
                user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toUnmodifiableSet()),
                user.getCreatedAt()
        );
    }
}
