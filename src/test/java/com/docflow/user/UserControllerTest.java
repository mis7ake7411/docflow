package com.docflow.user;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.stats.service.StatsService;
import com.docflow.test.TestSecurityConfig;
import com.docflow.user.controller.UserController;
import com.docflow.user.dto.CreateUserRequest;
import com.docflow.user.dto.CreateUserResponse;
import com.docflow.user.dto.UpdateUserRequest;
import com.docflow.user.dto.UserListItemResponse;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.service.UserService;
import com.docflow.common.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private UserService userService;

    @Test
    void getMyRecentViewsShouldReturnSuccessResponse() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("user")
                .email("user@example.com")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        DocflowUserPrincipal principal = new DocflowUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Mockito.when(statsService.getRecentViews(Mockito.anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users/me/recent-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void listUsersShouldReturnPagedResponse() throws Exception {
        UserListItemResponse item = UserListItemResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .role("USER")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
        PagedResponse<UserListItemResponse> response = PagedResponse.<UserListItemResponse>builder()
                .items(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        Mockito.when(userService.getUsers(0, 10, "alice")).thenReturn(response);

        mockMvc.perform(get("/api/users?page=0&size=10&keyword=alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createUserShouldReturnTempPassword() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setRole("USER");
        request.setStatus("ACTIVE");

        CreateUserResponse response = CreateUserResponse.builder()
                .tempPassword("Temp1234")
                .user(UserListItemResponse.builder()
                        .id(1L)
                        .username("alice")
                        .email("alice@example.com")
                        .role("USER")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();

        Mockito.when(userService.createUser(Mockito.any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tempPassword").value("Temp1234"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateUserShouldReturnUpdatedRole() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("MANAGER");
        request.setStatus("ACTIVE");

        UserListItemResponse response = UserListItemResponse.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .role("MANAGER")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(userService.updateUser(Mockito.eq(1L), Mockito.any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MANAGER"));
    }
}
