package com.docflow.document;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.common.response.PagedResponse;
import com.docflow.document.controller.DocumentController;
import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.DocumentShareItemResponse;
import com.docflow.document.dto.ShareDocumentRequest;
import com.docflow.document.service.DocumentService;
import com.docflow.test.TestSecurityConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        PagedResponse<DocumentResponse> response = PagedResponse.<DocumentResponse>builder()
                .items(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        Mockito.when(documentService.getPaged(0, 10, null)).thenReturn(response);

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSharedWithMeShouldReturnPagedResponse() throws Exception {
        PagedResponse<DocumentResponse> response = PagedResponse.<DocumentResponse>builder()
                .items(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        Mockito.when(documentService.getSharedWithMe(0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/documents/shared-with-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void getSharesShouldReturnShareList() throws Exception {
        DocumentShareItemResponse share = DocumentShareItemResponse.builder()
                .id(1L)
                .documentId(1L)
                .userId(2L)
                .username("bob")
                .permission("VIEW")
                .build();
        Mockito.when(documentService.getShares(1L)).thenReturn(List.of(share));

        mockMvc.perform(get("/api/documents/1/shares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("bob"));
    }

    @Test
    void createShareShouldReturnShareItem() throws Exception {
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setSharedWithUserId(2L);
        request.setPermission("VIEW");

        DocumentShareItemResponse response = DocumentShareItemResponse.builder()
                .id(1L)
                .documentId(1L)
                .userId(2L)
                .username("bob")
                .permission("VIEW")
                .build();
        Mockito.when(documentService.createShare(Mockito.eq(1L), Mockito.any(ShareDocumentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/documents/1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("VIEW"));
    }

    @Test
    void updateShareShouldReturnShareItem() throws Exception {
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setSharedWithUserId(2L);
        request.setPermission("EDIT");

        DocumentShareItemResponse response = DocumentShareItemResponse.builder()
                .id(1L)
                .documentId(1L)
                .userId(2L)
                .username("bob")
                .permission("EDIT")
                .build();
        Mockito.when(documentService.updateShare(Mockito.eq(1L), Mockito.eq(1L), Mockito.any(ShareDocumentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/documents/1/shares/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"));
    }

    @Test
    void deleteShareShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/documents/1/shares/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
