package com.docflow.stats.service;

import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;

import java.util.List;

public interface StatsService {

    List<HotDocumentItem> getHotDocuments();

    List<RecentViewItem> getRecentViews(Long userId);
}
