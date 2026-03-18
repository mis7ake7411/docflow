package com.docflow.stats.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供統計資料查詢 API。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Document statistics APIs")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    /**
     * 取得熱門文件列表。
     *
     * @return 熱門文件資料
     */
    @Operation(summary = "Get hot documents")
    @GetMapping("/hot-documents")
    public ApiResponse<List<HotDocumentItem>> getHotDocuments() {
        return ApiResponse.success(statsService.getHotDocuments());
    }
}
