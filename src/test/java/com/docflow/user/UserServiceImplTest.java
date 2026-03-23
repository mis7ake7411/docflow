package com.docflow.user;

import com.docflow.common.exception.BadRequestException;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import com.docflow.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void createUserShouldRejectDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setRole("USER");
        request.setStatus("ACTIVE");

        Mockito.when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createUserShouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setRole("USER");
        request.setStatus("ACTIVE");

        Mockito.when(userRepository.existsByUsername("alice")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateUserShouldChangeRoleAndStatus() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("MANAGER");
        request.setStatus("INACTIVE");

        UserListItemResponse response = userService.updateUser(1L, request);

        assertThat(response.getRole()).isEqualTo("MANAGER");
        assertThat(response.getStatus()).isEqualTo("INACTIVE");
    }
}
