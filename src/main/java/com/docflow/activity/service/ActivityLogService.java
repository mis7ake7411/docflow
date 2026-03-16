package com.docflow.activity.service;

import com.docflow.activity.entity.ActivityLog;

import java.util.List;
import java.util.Map;

public interface ActivityLogService {

    void log(Long userId, String targetType, Long targetId, String action, Map<String, Object> detail);

    List<ActivityLog> getRecentActivities();
}
