package com.docflow.user;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.stats.service.StatsService;
import com.docflow.user.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
    @WithMockUser
    void getMyRecentViewsShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(get("/api/users/me/recent-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
