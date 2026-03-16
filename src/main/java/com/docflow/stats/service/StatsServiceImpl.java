package com.docflow.stats.service;

import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.document.service.DocumentCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final DocumentCacheService documentCacheService;

    @Override
    public List<HotDocumentItem> getHotDocuments() {
        return documentCacheService.getHotDocuments();
    }

    @Override
    public List<RecentViewItem> getRecentViews(Long userId) {
        return documentCacheService.getRecentViews(userId);
    }
}
