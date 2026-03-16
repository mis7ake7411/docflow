package com.docflow.stats.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/hot-documents")
    public ApiResponse<List<HotDocumentItem>> getHotDocuments() {
        return ApiResponse.success(statsService.getHotDocuments());
    }
}
