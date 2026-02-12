package com.ibm.csvviewer.model;

import java.util.Map;

/**
 * Search filter for CSV data
 */
public class SearchFilter {
    private String globalSearch;
    private Map<String, String> columnSearch;
    private boolean inverseSearch;

    public SearchFilter() {
    }

    public SearchFilter(String globalSearch, Map<String, String> columnSearch, boolean inverseSearch) {
        this.globalSearch = globalSearch;
        this.columnSearch = columnSearch;
        this.inverseSearch = inverseSearch;
    }

    public String getGlobalSearch() {
        return globalSearch;
    }

    public void setGlobalSearch(String globalSearch) {
        this.globalSearch = globalSearch;
    }

    public Map<String, String> getColumnSearch() {
        return columnSearch;
    }

    public void setColumnSearch(Map<String, String> columnSearch) {
        this.columnSearch = columnSearch;
    }

    public boolean isInverseSearch() {
        return inverseSearch;
    }

    public void setInverseSearch(boolean inverseSearch) {
        this.inverseSearch = inverseSearch;
    }

    public boolean hasFilters() {
        return (globalSearch != null && !globalSearch.trim().isEmpty()) ||
               (columnSearch != null && !columnSearch.isEmpty());
    }
}

// Made with Bob
