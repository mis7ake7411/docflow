package com.docflow.user;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.stats.service.StatsService;
import com.docflow.test.TestSecurityConfig;
import com.docflow.user.controller.UserController;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

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
}
