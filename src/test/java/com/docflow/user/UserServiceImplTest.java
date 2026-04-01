package com.docflow.user;

import com.docflow.common.exception.BadRequestException;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.UpdateMyProfileRequest;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import com.docflow.user.service.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
        Mockito.when(userRepository.existsByEmailIgnoreCaseAndIdNot("manager@example.com", 1L)).thenReturn(false);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("manager@example.com");
        request.setRole("MANAGER");
        request.setStatus("INACTIVE");

        UserListItemResponse response = userService.updateUser(1L, request);

        assertThat(response.getEmail()).isEqualTo("manager@example.com");
        assertThat(response.getRole()).isEqualTo("MANAGER");
        assertThat(response.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updateUserShouldRejectDuplicateEmail() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Mockito.when(userRepository.existsByEmailIgnoreCaseAndIdNot("used@example.com", 1L)).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("used@example.com");
        request.setRole("USER");
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateMyProfileShouldOnlyChangeCurrentUserEmail() {
        User currentUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        DocflowUserPrincipal principal = new DocflowUserPrincipal(currentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        Mockito.when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 1L)).thenReturn(false);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setEmail("new@example.com");

        UserListItemResponse response = userService.updateMyProfile(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getShareCandidatesShouldExcludeCurrentUserAndFilterInactiveUsers() {
        User currentUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        DocflowUserPrincipal principal = new DocflowUserPrincipal(currentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        Mockito.when(userRepository.findShareCandidates(UserStatus.ACTIVE, 1L, "bob"))
                .thenReturn(List.of(User.builder()
                        .id(2L)
                        .username("bob")
                        .email("bob@example.com")
                        .passwordHash("hash")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        List<UserListItemResponse> response = userService.getShareCandidates("bob");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUsername()).isEqualTo("bob");
    }
}
