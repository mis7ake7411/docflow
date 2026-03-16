package com.docflow.activity.service;

import com.docflow.activity.entity.ActivityLog;
import com.docflow.activity.repository.ActivityLogRepository;
import com.docflow.user.entity.User;
import com.docflow.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void log(Long userId, String targetType, Long targetId, String action, Map<String, Object> detail) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .detailJson(toJson(detail))
                .build();

        activityLogRepository.save(activityLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivities() {
        return activityLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize activity detail", ex);
        }
    }
}
