package com.docflow.stats;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.test.TestSecurityConfig;
import com.docflow.stats.controller.StatsController;
import com.docflow.stats.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void getHotDocumentsShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(get("/api/stats/hot-documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
