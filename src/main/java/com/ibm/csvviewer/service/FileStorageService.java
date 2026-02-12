package com.ibm.csvviewer.service;

import com.ibm.csvviewer.model.CsvMetadata;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for managing file storage and metadata
 */
@ApplicationScoped
public class FileStorageService {
    
    private static final Logger LOGGER = Logger.getLogger(FileStorageService.class.getName());
    private static final String STORAGE_DIR = "data/uploads";
    private static final String METADATA_FILE = "data/metadata.json";
    
    private Path storageDirectory;
    private Path metadataFile;
    private Map<String, CsvMetadata> metadataCache;

    @PostConstruct
    public void init() {
        try {
            // Create storage directory if it doesn't exist
            storageDirectory = Paths.get(STORAGE_DIR);
            Files.createDirectories(storageDirectory);
            
            // Initialize metadata file
            metadataFile = Paths.get(METADATA_FILE);
            if (!Files.exists(metadataFile)) {
                Files.createDirectories(metadataFile.getParent());
                saveMetadata(new HashMap<>());
            }
            
            // Load metadata into cache
            loadMetadata();
            
            LOGGER.info("FileStorageService initialized. Storage directory: " + 
                       storageDirectory.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.severe("Failed to initialize FileStorageService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize file storage", e);
        }
    }

    /**
     * Store uploaded file
     */
    public String storeFile(InputStream inputStream, String originalFilename, long fileSize) 
            throws IOException {
        
        String fileId = UUID.randomUUID().toString();
        Path targetPath = storageDirectory.resolve(fileId + ".csv");
        
        // Copy file to storage
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        LOGGER.info("Stored file: " + originalFilename + " as " + fileId);
        
        return fileId;
    }

    /**
     * Save CSV metadata
     */
    public void saveMetadata(String fileId, CsvMetadata metadata) throws IOException {
        metadataCache.put(fileId, metadata);
        saveMetadata(metadataCache);
        LOGGER.info("Saved metadata for file: " + fileId);
    }

    /**
     * Get metadata for a specific file
     */
    public CsvMetadata getMetadata(String fileId) {
        return metadataCache.get(fileId);
    }

    /**
     * Get all file metadata
     */
    public List<CsvMetadata> getAllMetadata() {
        return new ArrayList<>(metadataCache.values());
    }

    /**
     * Get file path
     */
    public Path getFilePath(String fileId) {
        return storageDirectory.resolve(fileId + ".csv");
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileId) {
        return Files.exists(getFilePath(fileId)) && metadataCache.containsKey(fileId);
    }

    /**
     * Delete file and its metadata
     */
    public boolean deleteFile(String fileId) throws IOException {
        Path filePath = getFilePath(fileId);
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        
        metadataCache.remove(fileId);
        saveMetadata(metadataCache);
        
        LOGGER.info("Deleted file: " + fileId);
        return true;
    }

    /**
     * Load metadata from JSON file
     */
    private void loadMetadata() throws IOException {
        try (Jsonb jsonb = JsonbBuilder.create();
             Reader reader = Files.newBufferedReader(metadataFile)) {
            
            MetadataWrapper wrapper = jsonb.fromJson(reader, MetadataWrapper.class);
            metadataCache = new HashMap<>();
            
            if (wrapper != null && wrapper.files != null) {
                for (CsvMetadata metadata : wrapper.files) {
                    metadataCache.put(metadata.getId(), metadata);
                }
            }
            
            LOGGER.info("Loaded " + metadataCache.size() + " file metadata entries");
        } catch (Exception e) {
            LOGGER.warning("Failed to load metadata, starting with empty cache: " + e.getMessage());
            metadataCache = new HashMap<>();
        }
    }

    /**
     * Save metadata to JSON file
     */
    private void saveMetadata(Map<String, CsvMetadata> metadata) throws IOException {
        MetadataWrapper wrapper = new MetadataWrapper();
        wrapper.files = new ArrayList<>(metadata.values());
        
        try (Jsonb jsonb = JsonbBuilder.create();
             Writer writer = Files.newBufferedWriter(metadataFile)) {
            jsonb.toJson(wrapper, writer);
        } catch (Exception e) {
            throw new IOException("Failed to save metadata", e);
        }
    }

    /**
     * Wrapper class for JSON serialization
     */
    public static class MetadataWrapper {
        public List<CsvMetadata> files;
    }
}

// Made with Bob
