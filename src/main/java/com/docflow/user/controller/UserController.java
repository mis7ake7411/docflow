package com.docflow.user.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and history APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final StatsService statsService;

    @Operation(summary = "Get current user's recent viewed documents")
    @GetMapping("/me/recent-views")
    public ApiResponse<List<RecentViewItem>> getMyRecentViews() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(statsService.getRecentViews(userId));
    }
}
