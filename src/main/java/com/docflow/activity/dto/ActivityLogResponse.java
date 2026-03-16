package com.docflow.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ActivityLogResponse {

    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String action;
    private String detailJson;
    private LocalDateTime createdAt;
}
