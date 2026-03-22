package com.docflow.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PagedResponse<T> {
    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
