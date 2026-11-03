package com.smartprocure.user.service;

import com.smartprocure.common.audit.AuditService;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.exception.ResourceNotFoundException;
import com.smartprocure.common.exception.ValidationException;
import com.smartprocure.user.dto.ChangePasswordRequest;
import com.smartprocure.user.dto.UpdateProfileRequest;
import com.smartprocure.user.dto.UserDto;
import com.smartprocure.user.entity.Role;
import com.smartprocure.user.entity.RoleName;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import com.smartprocure.user.repository.RoleRepository;
import com.smartprocure.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private RoleRepository  roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService    auditService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role employeeRole = new Role();
        employeeRole.setId(1L);
        employeeRole.setName(RoleName.EMPLOYEE);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encodedOldPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setDepartment("IT");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRoles(new HashSet<>(Set.of(employeeRole)));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test @DisplayName("findById: existing user → returns UserDto")
    void findById_existingUser_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.roles()).containsExactly("EMPLOYEE");
    }

    @Test @DisplayName("findById: non-existent user → ResourceNotFoundException")
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test @DisplayName("updateProfile: valid request → fields updated and audit logged")
    void updateProfile_validRequest_updatesFields() {
        var req = new UpdateProfileRequest("Jane", "Doe", "Engineering", "+1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserDto result = userService.updateProfile(1L, req);

        assertThat(result).isNotNull();
        verify(auditService).log(eq("USER"), eq(1L), eq("UPDATE_PROFILE"),
                anyString(), anyString(), anyString());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test @DisplayName("changePassword: correct current + matching confirmation → success")
    void changePassword_valid_updatesHash() {
        var req = new ChangePasswordRequest("oldPass", "New@Password1!", "New@Password1!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("New@Password1!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any())).thenReturn(testUser);

        assertThatCode(() -> userService.changePassword(1L, req)).doesNotThrowAnyException();
        verify(passwordEncoder).encode("New@Password1!");
        verify(auditService).log(eq("USER"), eq(1L), eq("CHANGE_PASSWORD"), anyString());
    }

    @Test @DisplayName("changePassword: wrong current password → INVALID_CREDENTIALS")
    void changePassword_wrongCurrent_throws() {
        var req = new ChangePasswordRequest("wrongOld", "New@Password1!", "New@Password1!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "encodedOldPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test @DisplayName("changePassword: mismatched confirmation → ValidationException")
    void changePassword_mismatchedConfirm_throws() {
        var req = new ChangePasswordRequest("oldPass", "New@Password1!", "Different@2!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.changePassword(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("do not match");
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test @DisplayName("updateStatus: ADMIN deactivates user → status updated and audited")
    void updateStatus_deactivate_updatesStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserDto result = userService.updateStatus(1L, UserStatus.INACTIVE);

        assertThat(result).isNotNull();
        verify(auditService).log(eq("USER"), eq(1L), eq("UPDATE_STATUS"),
                anyString(), eq("ACTIVE"), eq("INACTIVE"));
    }

    // ── isCurrentUser ─────────────────────────────────────────────────────────

    @Test @DisplayName("isCurrentUser: unauthenticated → false (no SecurityContext user)")
    void isCurrentUser_unauthenticated_returnsFalse() {
        // SecurityContextHolder is empty in unit tests → returns false
        assertThat(userService.isCurrentUser(1L)).isFalse();
    }
}
