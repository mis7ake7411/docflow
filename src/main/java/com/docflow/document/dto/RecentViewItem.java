package com.docflow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecentViewItem {

    private Long documentId;
    private String title;
    private String status;
    private Double score;
}
