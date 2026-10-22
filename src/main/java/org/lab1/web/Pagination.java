package org.lab1.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    public static final String TOTAL_PAGES_HEADER = "X-Total-Pages";

    private Pagination() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(boundedPage, boundedSize, sort);
    }
}
