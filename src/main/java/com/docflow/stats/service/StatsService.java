package com.docflow.stats.service;

import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;

import java.util.List;

/**
 * 提供儀表板統計資料查詢能力。
 */
public interface StatsService {

    /**
     * 取得熱門文件列表。
     *
     * @return 熱門文件資料
     */
    List<HotDocumentItem> getHotDocuments();

    /**
     * 取得指定使用者最近瀏覽的文件列表。
     *
     * @param userId 使用者編號
     * @return 最近瀏覽文件資料
     */
    List<RecentViewItem> getRecentViews(Long userId);
}
