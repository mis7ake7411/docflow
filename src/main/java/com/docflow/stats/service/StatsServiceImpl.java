package com.docflow.stats.service;

import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.document.service.DocumentCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link StatsService} 的預設實作，委派快取服務提供統計資料。
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final DocumentCacheService documentCacheService;

    /**
     * 取得熱門文件列表。
     *
     * @return 熱門文件資料
     */
    @Override
    public List<HotDocumentItem> getHotDocuments() {
        return documentCacheService.getHotDocuments();
    }

    /**
     * 取得指定使用者最近瀏覽的文件列表。
     *
     * @param userId 使用者編號
     * @return 最近瀏覽文件資料
     */
    @Override
    public List<RecentViewItem> getRecentViews(Long userId) {
        return documentCacheService.getRecentViews(userId);
    }
}
