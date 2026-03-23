package com.docflow.activity.service;

import com.docflow.activity.entity.ActivityLog;
import com.docflow.activity.repository.ActivityLogRepository;
import com.docflow.user.entity.User;
import com.docflow.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 新增一筆活動紀錄，並在提供使用者編號時關聯對應使用者。
     *
     * @param userId 執行動作的使用者編號，可為 {@code null}
     * @param targetType 紀錄目標類型
     * @param targetId 紀錄目標編號
     * @param action 執行的動作名稱
     * @param detail 額外明細資料，會序列化為 JSON 儲存
     */
    @Override
    @Transactional
    public void log(Long userId, String targetType, Long targetId, String action, Map<String, Object> detail) {
        log.debug("Writing activity log: userId={}, targetType={}, targetId={}, action={}",
                userId, targetType, targetId, action);
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
        log.debug("Activity log persisted: action={}, targetType={}, targetId={}", action, targetType, targetId);
    }

    /**
     * 取得最新的活動紀錄列表。
     *
     * @return 依建立時間由新到舊排序的最新 100 筆活動紀錄
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivities() {
        return activityLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    /**
     * 取得分頁的活動紀錄。
     *
     * @param page 頁碼（0-based）
     * @param size 每頁筆數
     * @return 分頁活動紀錄
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getPaged(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 將活動明細資料序列化為 JSON 字串。
     *
     * @param detail 活動明細資料
     * @return 序列化後的 JSON 字串；若明細為空則回傳 {@code null}
     * @throws IllegalStateException 當明細資料無法序列化時拋出
     */
    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize activity detail: detail={}", detail, ex);
            throw new IllegalStateException("Failed to serialize activity detail", ex);
        }
    }
}
