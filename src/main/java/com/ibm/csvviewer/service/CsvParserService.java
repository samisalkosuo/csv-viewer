package com.ibm.csvviewer.service;

import com.ibm.csvviewer.model.CsvData;
import com.ibm.csvviewer.model.CsvMetadata;
import com.ibm.csvviewer.util.DelimiterDetector;
import com.ibm.csvviewer.util.StreamingCsvReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for parsing CSV files
 */
@ApplicationScoped
public class CsvParserService {
    
    private static final Logger LOGGER = Logger.getLogger(CsvParserService.class.getName());
    
    @Inject
    private FileStorageService fileStorageService;

    /**
     * Process uploaded CSV file
     */
    public CsvMetadata processUploadedFile(InputStream inputStream, String originalFilename, 
                                          long fileSize) throws IOException {
        
        // Read file content into byte array for multiple passes
        byte[] fileContent = inputStream.readAllBytes();
        
        // Detect delimiter
        char delimiter;
        try (InputStream delimiterStream = new ByteArrayInputStream(fileContent)) {
            delimiter = DelimiterDetector.detectDelimiter(delimiterStream);
            LOGGER.info("Detected delimiter: " + DelimiterDetector.getDelimiterName(delimiter));
        }
        
        // Store file
        String fileId;
        try (InputStream storeStream = new ByteArrayInputStream(fileContent)) {
            fileId = fileStorageService.storeFile(storeStream, originalFilename, fileSize);
        }
        
        // Parse CSV to get metadata
        Path filePath = fileStorageService.getFilePath(fileId);
        List<String> columns = StreamingCsvReader.readHeaders(filePath, delimiter);
        long rowCount = StreamingCsvReader.countRows(filePath, delimiter);
        
        // Create metadata
        CsvMetadata metadata = new CsvMetadata(
            fileId,
            originalFilename,
            Instant.now(),
            fileSize,
            rowCount,
            columns.size(),
            columns,
            delimiter
        );
        
        // Save metadata
        fileStorageService.saveMetadata(fileId, metadata);
        
        LOGGER.info("Processed CSV file: " + originalFilename + 
                   " (rows: " + rowCount + ", columns: " + columns.size() + ")");
        
        return metadata;
    }

    /**
     * Get paginated CSV data with optional sorting and filtering
     */
    public CsvData getCsvData(String fileId, int page, int pageSize,
                             String sortColumn, String sortOrder,
                             String globalSearch, Map<String, String> columnSearch,
                             boolean inverseSearch) throws IOException {
        
        if (!fileStorageService.fileExists(fileId)) {
            throw new IOException("File not found: " + fileId);
        }
        
        CsvMetadata metadata = fileStorageService.getMetadata(fileId);
        Path filePath = fileStorageService.getFilePath(fileId);
        
        boolean sortAscending = !"desc".equalsIgnoreCase(sortOrder);
        
        // Get filtered row count
        long totalRows;
        if (hasFilters(globalSearch, columnSearch)) {
            totalRows = StreamingCsvReader.countFilteredRows(
                filePath, metadata.getDelimiter(), 
                globalSearch, columnSearch, inverseSearch
            );
        } else {
            totalRows = metadata.getRowCount();
        }
        
        // Read page data
        List<List<String>> rows = StreamingCsvReader.readPage(
            filePath, metadata.getDelimiter(),
            page, pageSize,
            sortColumn, sortAscending,
            globalSearch, columnSearch, inverseSearch
        );
        
        return new CsvData(metadata.getColumns(), rows, totalRows, page, pageSize);
    }

    /**
     * Get CSV data for download (all filtered/sorted rows)
     */
    public List<List<String>> getCsvDataForDownload(String fileId,
                                                     String sortColumn, String sortOrder,
                                                     String globalSearch, 
                                                     Map<String, String> columnSearch,
                                                     boolean inverseSearch) throws IOException {
        
        if (!fileStorageService.fileExists(fileId)) {
            throw new IOException("File not found: " + fileId);
        }
        
        CsvMetadata metadata = fileStorageService.getMetadata(fileId);
        Path filePath = fileStorageService.getFilePath(fileId);
        
        boolean sortAscending = !"desc".equalsIgnoreCase(sortOrder);
        
        // Get filtered row count
        long totalRows;
        if (hasFilters(globalSearch, columnSearch)) {
            totalRows = StreamingCsvReader.countFilteredRows(
                filePath, metadata.getDelimiter(), 
                globalSearch, columnSearch, inverseSearch
            );
        } else {
            totalRows = metadata.getRowCount();
        }
        
        // Read all filtered data (use large page size)
        return StreamingCsvReader.readPage(
            filePath, metadata.getDelimiter(),
            0, (int) totalRows,
            sortColumn, sortAscending,
            globalSearch, columnSearch, inverseSearch
        );
    }

    /**
     * Check if any filters are applied
     */
    private boolean hasFilters(String globalSearch, Map<String, String> columnSearch) {
        return (globalSearch != null && !globalSearch.trim().isEmpty()) ||
               (columnSearch != null && !columnSearch.isEmpty());
    }
}

// Made with Bob
