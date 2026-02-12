package com.ibm.csvviewer.model;

import java.util.List;

/**
 * Paginated CSV data response
 */
public class CsvData {
    private List<String> columns;
    private List<List<String>> rows;
    private long totalRows;
    private int page;
    private int pageSize;
    private int totalPages;

    public CsvData() {
    }

    public CsvData(List<String> columns, List<List<String>> rows, long totalRows, 
                   int page, int pageSize) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalRows / pageSize);
    }

    // Getters and Setters
    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

// Made with Bob
