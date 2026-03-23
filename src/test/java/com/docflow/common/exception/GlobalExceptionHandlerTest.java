package com.docflow.common.exception;

import com.docflow.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleForbiddenShouldReturn403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        String message = "\u7121\u6b0a\u9650\u64cd\u4f5c\u6b64\u6587\u4ef6";
        ForbiddenException ex = new ForbiddenException(message);

        ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
