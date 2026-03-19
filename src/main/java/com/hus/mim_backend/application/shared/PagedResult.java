package com.hus.mim_backend.application.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic paged response model used by application use cases.
 */
public class PagedResult<T> {
    private List<T> content = new ArrayList<>();
    private PageInfo pageInfo = new PageInfo();

    public PagedResult() {
    }

    public PagedResult(List<T> content, PageInfo pageInfo) {
        this.content = content == null ? new ArrayList<>() : content;
        this.pageInfo = pageInfo == null ? new PageInfo() : pageInfo;
    }

    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int totalPages = totalElements <= 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        return new PagedResult<>(content, new PageInfo(safePage, safeSize, totalElements, totalPages));
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content == null ? new ArrayList<>() : content;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo == null ? new PageInfo() : pageInfo;
    }

    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public PageInfo() {
        }

        public PageInfo(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }
}
