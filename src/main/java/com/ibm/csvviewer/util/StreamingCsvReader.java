package com.ibm.csvviewer.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Streaming CSV reader for efficient processing of large files
 */
public class StreamingCsvReader {

    /**
     * Read CSV headers
     */
    public static List<String> readHeaders(Path filePath, char delimiter) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            return new ArrayList<>(parser.getHeaderNames());
        }
    }

    /**
     * Count total rows in CSV file (excluding header)
     */
    public static long countRows(Path filePath, char delimiter) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            long count = 0;
            for (CSVRecord record : parser) {
                count++;
            }
            return count;
        }
    }

    /**
     * Read paginated CSV data with optional sorting and filtering
     */
    public static List<List<String>> readPage(Path filePath, char delimiter, 
                                              int page, int pageSize,
                                              String sortColumn, boolean sortAscending,
                                              String globalSearch, 
                                              java.util.Map<String, String> columnSearch,
                                              boolean inverseSearch) throws IOException {
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<List<String>> allRows = new ArrayList<>();
        List<String> headers;

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            headers = new ArrayList<>(parser.getHeaderNames());
            
            // Read and filter all rows
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.add(record.get(i));
                }
                
                // Apply filters
                if (matchesFilter(row, headers, globalSearch, columnSearch, inverseSearch)) {
                    allRows.add(row);
                }
            }
        }

        // Sort if requested
        if (sortColumn != null && !sortColumn.isEmpty() && headers.contains(sortColumn)) {
            int sortColumnIndex = headers.indexOf(sortColumn);
            allRows.sort((row1, row2) -> {
                String val1 = row1.get(sortColumnIndex);
                String val2 = row2.get(sortColumnIndex);
                
                // Try numeric comparison first
                try {
                    double num1 = Double.parseDouble(val1);
                    double num2 = Double.parseDouble(val2);
                    int result = Double.compare(num1, num2);
                    return sortAscending ? result : -result;
                } catch (NumberFormatException e) {
                    // Fall back to string comparison
                    int result = val1.compareToIgnoreCase(val2);
                    return sortAscending ? result : -result;
                }
            });
        }

        // Apply pagination
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allRows.size());
        
        if (startIndex >= allRows.size()) {
            return new ArrayList<>();
        }
        
        return allRows.subList(startIndex, endIndex);
    }

    /**
     * Check if a row matches the search filters
     */
    private static boolean matchesFilter(List<String> row, List<String> headers,
                                        String globalSearch,
                                        java.util.Map<String, String> columnSearch,
                                        boolean inverseSearch) {
        boolean matches = true;

        // Global search - check all columns
        if (globalSearch != null && !globalSearch.trim().isEmpty()) {
            String searchLower = globalSearch.toLowerCase();
            boolean found = row.stream()
                    .anyMatch(cell -> cell.toLowerCase().contains(searchLower));
            matches = found;
        }

        // Column-specific search
        if (columnSearch != null && !columnSearch.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : columnSearch.entrySet()) {
                String columnName = entry.getKey();
                String searchValue = entry.getValue();
                
                if (searchValue != null && !searchValue.trim().isEmpty()) {
                    int columnIndex = headers.indexOf(columnName);
                    if (columnIndex >= 0 && columnIndex < row.size()) {
                        String cellValue = row.get(columnIndex).toLowerCase();
                        String searchLower = searchValue.toLowerCase();
                        
                        if (!cellValue.contains(searchLower)) {
                            matches = false;
                            break;
                        }
                    }
                }
            }
        }

        // Apply inverse search if enabled
        return inverseSearch ? !matches : matches;
    }

    /**
     * Get filtered row count
     */
    public static long countFilteredRows(Path filePath, char delimiter,
                                         String globalSearch,
                                         java.util.Map<String, String> columnSearch,
                                         boolean inverseSearch) throws IOException {
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        long count = 0;
        List<String> headers;

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            headers = new ArrayList<>(parser.getHeaderNames());
            
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.add(record.get(i));
                }
                
                if (matchesFilter(row, headers, globalSearch, columnSearch, inverseSearch)) {
                    count++;
                }
            }
        }

        return count;
    }
}

// Made with Bob
