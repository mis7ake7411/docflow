package com.docflow.document;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.document.controller.DocumentController;
import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @Test
    void createShouldReturnSuccessResponse() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setFolderId(null);
        request.setTitle("API Spec");
        request.setDescription("Initial draft");
        request.setStatus("DRAFT");

        DocumentResponse response = DocumentResponse.builder()
                .id(1L)
                .folderId(null)
                .title("API Spec")
                .description("Initial draft")
                .version(1)
                .status("DRAFT")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(documentService.create(Mockito.any(CreateDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("API Spec"));
    }

    @Test
    void getAllShouldReturnSuccessResponse() throws Exception {
        Mockito.when(documentService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
