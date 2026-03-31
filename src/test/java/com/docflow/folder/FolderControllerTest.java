package com.docflow.folder;

import com.docflow.common.exception.GlobalExceptionHandler;
import com.docflow.test.TestSecurityConfig;
import com.docflow.folder.controller.FolderController;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.ReorderFoldersRequest;
import com.docflow.folder.service.FolderService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FolderService folderService;

    @Test
    void createShouldReturnSuccessResponse() throws Exception {
        CreateFolderRequest request = new CreateFolderRequest();
        request.setName("Root Folder");
        request.setParentId(null);

        FolderResponse response = FolderResponse.builder()
                .id(1L)
                .name("Root Folder")
                .parentId(null)
                .sortOrder(1)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(folderService.create(Mockito.any(CreateFolderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Root Folder"));
    }

    @Test
    void reorderShouldReturnSuccessResponse() throws Exception {
        ReorderFoldersRequest request = new ReorderFoldersRequest();
        request.setParentId(null);
        request.setOrderedFolderIds(java.util.List.of(3L, 1L, 2L));

        mockMvc.perform(put("/api/folders/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void treeShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(get("/api/folders/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
