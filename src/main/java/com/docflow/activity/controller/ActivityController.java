package com.docflow.activity.controller;

import com.docflow.activity.dto.ActivityLogResponse;
import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供活動紀錄查詢 API。
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "Audit trail and activity log APIs")
@SecurityRequirement(name = "bearerAuth")
public class ActivityController {

    private final ActivityLogService activityLogService;

    /**
     * 取得最近的活動紀錄列表。
     *
     * @return 活動紀錄回應
     */
    @Operation(summary = "Get recent activity logs")
    @GetMapping
    public ApiResponse<List<ActivityLogResponse>> getActivities() {
        List<ActivityLogResponse> responses = activityLogService.getRecentActivities().stream()
                .map(activity -> ActivityLogResponse.builder()
                        .id(activity.getId())
                        .userId(activity.getUser() != null ? activity.getUser().getId() : null)
                        .targetType(activity.getTargetType())
                        .targetId(activity.getTargetId())
                        .action(activity.getAction())
                        .detailJson(activity.getDetailJson())
                        .createdAt(activity.getCreatedAt())
                        .build())
                .toList();
        return ApiResponse.success(responses);
    }
}
