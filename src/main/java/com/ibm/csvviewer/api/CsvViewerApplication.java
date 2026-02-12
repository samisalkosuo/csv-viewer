package com.ibm.csvviewer.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application configuration
 */
@ApplicationPath("/api")
public class CsvViewerApplication extends Application {
    // No additional configuration needed
    // All REST resources will be automatically discovered
}

// Made with Bob
