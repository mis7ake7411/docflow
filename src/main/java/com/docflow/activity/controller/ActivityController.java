package com.docflow.activity.controller;

import com.docflow.activity.dto.ActivityLogResponse;
import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityLogService activityLogService;

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
