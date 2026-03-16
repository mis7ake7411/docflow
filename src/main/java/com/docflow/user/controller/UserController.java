package com.docflow.user.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final StatsService statsService;

    @GetMapping("/me/recent-views")
    public ApiResponse<List<RecentViewItem>> getMyRecentViews() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(statsService.getRecentViews(userId));
    }
}
