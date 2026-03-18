package com.docflow.activity.repository;

import com.docflow.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 提供活動紀錄資料存取操作。
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * 取得最新 100 筆活動紀錄。
     *
     * @return 依建立時間由新到舊排序的活動紀錄
     */
    List<ActivityLog> findTop100ByOrderByCreatedAtDesc();
}
