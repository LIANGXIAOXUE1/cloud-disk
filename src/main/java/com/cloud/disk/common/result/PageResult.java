package com.cloud.disk.common.result;

import java.util.List;

public class PageResult<T> {
    private List<T> list;
    private int total;
    private int pageSize;
    private int currentPage;

    public PageResult() {}

    public PageResult(List<T> list, int total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
}