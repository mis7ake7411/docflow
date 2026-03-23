package com.docflow.activity;

import com.docflow.activity.controller.ActivityController;
import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.test.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityLogService activityLogService;

    @Test
    void getActivitiesShouldReturnSuccessResponse() throws Exception {
        Mockito.when(activityLogService.getPaged(Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
