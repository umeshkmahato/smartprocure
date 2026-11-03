package com.smartprocure.user.service;

import com.smartprocure.common.audit.AuditService;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.exception.ResourceNotFoundException;
import com.smartprocure.common.exception.ValidationException;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.common.util.SecurityUtils;
import com.smartprocure.user.dto.ChangePasswordRequest;
import com.smartprocure.user.dto.UpdateProfileRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.entity.Role;
import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.repository.RoleRepository;
import com.smartprocure.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("userService")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService    auditService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return UserDto.fromEntity(requireUser(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> findAll(int page, int size, String sortParam) {
        return PageResponse.from(
                userRepository.findAll(PageRequest.of(page, size, parseSort(sortParam)))
                              .map(UserDto::fromEntity));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public UserDto updateProfile(Long id, UpdateProfileRequest req) {
        User user    = requireUser(id);
        String oldName = user.getFullName();

        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setDepartment(req.department());
        user.setPhone(req.phone());

        User saved = userRepository.save(user);
        auditService.log("USER", id, "UPDATE_PROFILE", "Profile updated", oldName, saved.getFullName());
        return UserDto.fromEntity(saved);
    }

    public void changePassword(Long id, ChangePasswordRequest req) {
        if (!req.newPassword().equals(req.confirmNewPassword())) {
            throw new ValidationException("New password and confirmation do not match");
        }
        User user = requireUser(id);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        auditService.log("USER", id, "CHANGE_PASSWORD", "Password changed");
    }

    public UserDto updateStatus(Long id, UserStatus newStatus) {
        User user = requireUser(id);
        String old = user.getStatus().name();
        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        auditService.log("USER", id, "UPDATE_STATUS", "Status changed", old, newStatus.name());
        return UserDto.fromEntity(saved);
    }

    public UserDto updateRoles(Long id, Set<String> roleNames) {
        User user = requireUser(id);

        Set<RoleName> parsed = roleNames.stream()
                .map(n -> {
                    try {
                        return RoleName.valueOf(n.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Unknown role: " + n);
                    }
                })
                .collect(Collectors.toSet());

        List<Role> roles = roleRepository.findByNameIn(parsed);
        user.setRoles(new HashSet<>(roles));
        User saved = userRepository.save(user);
        auditService.log("USER", id, "UPDATE_ROLES", "Roles updated to: " + roleNames);
        return UserDto.fromEntity(saved);
    }

    // ── Current-user helpers ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return UserDto.fromEntity(
                userRepository.findByUsernameOrEmail(username, username)
                              .orElseThrow(() -> new ResourceNotFoundException(
                                      "User", "username", username)));
    }

    /**
     * Called from {@code @PreAuthorize("@userService.isCurrentUser(#id)")} expressions
     * in {@link com.smartprocure.user.controller.UserController}.
     */
    @Transactional(readOnly = true)
    public boolean isCurrentUser(Long id) {
        return SecurityUtils.getCurrentUsername()
                .flatMap(u -> userRepository.findByUsernameOrEmail(u, u))
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private Sort parseSort(String sortParam) {
        String[] parts = sortParam.split(",");
        String   field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
