package com.ibm.csvviewer.model;

import java.time.Instant;
import java.util.List;

/**
 * Metadata for an uploaded CSV file
 */
public class CsvMetadata {
    private String id;
    private String originalName;
    private Instant uploadDate;
    private long size;
    private long rowCount;
    private int columnCount;
    private List<String> columns;
    private char delimiter;

    public CsvMetadata() {
    }

    public CsvMetadata(String id, String originalName, Instant uploadDate, long size, 
                      long rowCount, int columnCount, List<String> columns, char delimiter) {
        this.id = id;
        this.originalName = originalName;
        this.uploadDate = uploadDate;
        this.size = size;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.columns = columns;
        this.delimiter = delimiter;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Instant getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Instant uploadDate) {
        this.uploadDate = uploadDate;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }
}

// Made with Bob
