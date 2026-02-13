package com.ibm.csvviewer.api;

import com.ibm.csvviewer.model.CsvData;
import com.ibm.csvviewer.model.CsvMetadata;
import com.ibm.csvviewer.service.CsvParserService;
import com.ibm.csvviewer.service.FileStorageService;
import jakarta.inject.Inject;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * REST API for CSV operations
 */
@Path("/csv")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CsvResource {
    
    private static final Logger LOGGER = Logger.getLogger(CsvResource.class.getName());
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    
    @Inject
    private CsvParserService csvParserService;
    
    @Inject
    private FileStorageService fileStorageService;

    /**
     * Upload CSV file
     * POST /api/csv/upload
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@Context HttpServletRequest request) {
        LOGGER.info("=== Upload request received ===");
        try {
            LOGGER.info("Getting file part from request...");
            Part filePart = request.getPart("file");
            
            if (filePart == null) {
                LOGGER.warning("No file part found in request");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "No file provided"))
                        .build();
            }
            
            LOGGER.info("File part found: " + filePart.getName());
            
            // Get filename
            String filename = getSubmittedFileName(filePart);
            LOGGER.info("Filename: " + filename);
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                LOGGER.warning("Invalid file type: " + filename);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Only CSV files are allowed"))
                        .build();
            }
            
            // Get file content
            LOGGER.info("Reading file content...");
            InputStream inputStream = filePart.getInputStream();
            byte[] fileContent = inputStream.readAllBytes();
            long fileSize = fileContent.length;
            LOGGER.info("File size: " + fileSize + " bytes");
            
            // Check file size
            if (fileSize > MAX_FILE_SIZE) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "File size exceeds maximum allowed size (500MB)"))
                        .build();
            }
            
            if (fileSize == 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "File is empty"))
                        .build();
            }
            
            // Process file
            LOGGER.info("Processing CSV file...");
            CsvMetadata metadata = csvParserService.processUploadedFile(
                new java.io.ByteArrayInputStream(fileContent), filename, fileSize
            );
            
            LOGGER.info("File uploaded successfully: " + filename + ", ID: " + metadata.getId());
            
            return Response.ok(metadata).build();
            
        } catch (Exception e) {
            LOGGER.severe("Error uploading file: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to upload file: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * List all uploaded files
     * GET /api/csv/list
     */
    @GET
    @Path("/list")
    public Response listFiles() {
        LOGGER.info("=== List files request received ===");
        try {
            List<CsvMetadata> files = fileStorageService.getAllMetadata();
            LOGGER.info("Found " + files.size() + " files");
            return Response.ok(files).build();
        } catch (Exception e) {
            LOGGER.severe("Error listing files: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to list files: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get file metadata
     * GET /api/csv/{fileId}/metadata
     */
    @GET
    @Path("/{fileId}/metadata")
    public Response getMetadata(@PathParam("fileId") String fileId) {
        try {
            if (!fileStorageService.fileExists(fileId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "File not found"))
                        .build();
            }
            
            CsvMetadata metadata = fileStorageService.getMetadata(fileId);
            return Response.ok(metadata).build();
            
        } catch (Exception e) {
            LOGGER.severe("Error getting metadata: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get metadata: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get paginated CSV data
     * GET /api/csv/{fileId}/data
     */
    @GET
    @Path("/{fileId}/data")
    public Response getData(
            @PathParam("fileId") String fileId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("25") int pageSize,
            @QueryParam("sortColumn") String sortColumn,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
            @QueryParam("globalSearch") String globalSearch,
            @QueryParam("inverseSearch") @DefaultValue("false") boolean inverseSearch,
            @Context HttpServletRequest request) {
        
        LOGGER.info("=== Get data request for fileId: " + fileId + " ===");
        try {
            if (!fileStorageService.fileExists(fileId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "File not found"))
                        .build();
            }
            
            // Parse column search parameters (format: col_ColumnName=value)
            Map<String, String> columnSearch = new HashMap<>();
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                if (paramName.startsWith("col_") && entry.getValue().length > 0) {
                    String columnName = paramName.substring(4); // Remove "col_" prefix
                    String searchValue = entry.getValue()[0];
                    if (searchValue != null && !searchValue.trim().isEmpty()) {
                        columnSearch.put(columnName, searchValue);
                        LOGGER.info("Column search: " + columnName + " = " + searchValue);
                    }
                }
            }
            
            LOGGER.info("Column search map size: " + columnSearch.size());
            
            CsvData data = csvParserService.getCsvData(
                fileId, page, pageSize, sortColumn, sortOrder,
                globalSearch, columnSearch, inverseSearch
            );
            
            return Response.ok(data).build();
            
        } catch (Exception e) {
            LOGGER.severe("Error getting CSV data: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get data: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Download filtered/sorted CSV data
     * GET /api/csv/{fileId}/download
     */
    @GET
    @Path("/{fileId}/download")
    @Produces("text/csv")
    public Response downloadData(
            @PathParam("fileId") String fileId,
            @QueryParam("sortColumn") String sortColumn,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
            @QueryParam("globalSearch") String globalSearch,
            @QueryParam("inverseSearch") @DefaultValue("false") boolean inverseSearch,
            @QueryParam("hiddenColumns") String hiddenColumns) {
        
        try {
            if (!fileStorageService.fileExists(fileId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("File not found")
                        .build();
            }
            
            CsvMetadata metadata = fileStorageService.getMetadata(fileId);
            Map<String, String> columnSearch = new HashMap<>();
            
            // Parse hidden columns
            List<String> hiddenColumnsList = new java.util.ArrayList<>();
            if (hiddenColumns != null && !hiddenColumns.trim().isEmpty()) {
                hiddenColumnsList = java.util.Arrays.asList(hiddenColumns.split(","));
                LOGGER.info("Hidden columns: " + hiddenColumnsList);
            }
            
            List<List<String>> rows = csvParserService.getCsvDataForDownload(
                fileId, sortColumn, sortOrder, globalSearch, columnSearch, inverseSearch
            );
            
            // Filter columns based on hidden columns
            List<String> allColumns = metadata.getColumns();
            List<Integer> visibleColumnIndices = new java.util.ArrayList<>();
            List<String> visibleColumns = new java.util.ArrayList<>();
            
            for (int i = 0; i < allColumns.size(); i++) {
                if (!hiddenColumnsList.contains(allColumns.get(i))) {
                    visibleColumnIndices.add(i);
                    visibleColumns.add(allColumns.get(i));
                }
            }
            
            // Create streaming output for CSV
            StreamingOutput stream = output -> {
                try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
                    // Write header (only visible columns)
                    writer.write(String.join(",", visibleColumns));
                    writer.write("\n");
                    
                    // Write rows (only visible columns)
                    for (List<String> row : rows) {
                        List<String> visibleRow = new java.util.ArrayList<>();
                        for (int index : visibleColumnIndices) {
                            if (index < row.size()) {
                                visibleRow.add(row.get(index));
                            }
                        }
                        
                        // Escape values that contain commas or quotes
                        List<String> escapedRow = visibleRow.stream()
                            .map(cell -> {
                                if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                                    return "\"" + cell.replace("\"", "\"\"") + "\"";
                                }
                                return cell;
                            })
                            .toList();
                        writer.write(String.join(",", escapedRow));
                        writer.write("\n");
                    }
                }
            };
            
            String filename = metadata.getOriginalName().replace(".csv", "_filtered.csv");
            
            return Response.ok(stream)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
            
        } catch (Exception e) {
            LOGGER.severe("Error downloading CSV: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to download file: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Delete file
     * DELETE /api/csv/{fileId}
     */
    @DELETE
    @Path("/{fileId}")
    public Response deleteFile(@PathParam("fileId") String fileId) {
        try {
            if (!fileStorageService.fileExists(fileId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "File not found"))
                        .build();
            }
            
            fileStorageService.deleteFile(fileId);
            
            return Response.ok(Map.of("message", "File deleted successfully")).build();
            
        } catch (Exception e) {
            LOGGER.severe("Error deleting file: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to delete file: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Extract filename from multipart Part
     */
    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim()
                        .replace("\"", "");
            }
        }
        return null;
    }
}

// Made with Bob
