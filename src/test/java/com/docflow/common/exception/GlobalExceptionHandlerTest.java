package com.docflow.common.exception;

import com.docflow.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleForbiddenShouldReturn403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ForbiddenException ex = new ForbiddenException("無權限操作此文件");

        ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("無權限操作此文件");
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}