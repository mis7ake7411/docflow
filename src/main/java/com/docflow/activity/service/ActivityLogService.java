package com.docflow.activity.service;

import com.docflow.activity.entity.ActivityLog;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 提供活動紀錄的寫入與查詢能力。
 */
public interface ActivityLogService {

    /**
     * 寫入一筆活動紀錄。
     *
     * @param userId 執行動作的使用者編號，可為 {@code null}
     * @param targetType 紀錄目標類型
     * @param targetId 紀錄目標編號
     * @param action 執行的動作名稱
     * @param detail 額外明細資料
     */
    void log(Long userId, String targetType, Long targetId, String action, Map<String, Object> detail);

    /**
     * 取得最新的活動紀錄列表。
     *
     * @return 依建立時間由新到舊排序的活動紀錄
     */
    List<ActivityLog> getRecentActivities();

    /**
     * 取得分頁的活動紀錄。
     *
     * @param page 頁碼（0-based）
     * @param size 每頁筆數
     * @return 分頁活動紀錄
     */
    Page<ActivityLog> getPaged(int page, int size);
}
