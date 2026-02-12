package com.ibm.csvviewer.util;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DelimiterDetector
 */
class DelimiterDetectorTest {

    @Test
    void testDetectCommaDelimiter() throws IOException {
        String csvContent = """
            Name,Age,City
            John,30,New York
            Jane,25,Los Angeles
            Bob,35,Chicago
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals(',', delimiter, "Should detect comma delimiter");
    }

    @Test
    void testDetectSemicolonDelimiter() throws IOException {
        String csvContent = """
            Name;Age;City
            John;30;New York
            Jane;25;Los Angeles
            Bob;35;Chicago
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals(';', delimiter, "Should detect semicolon delimiter");
    }

    @Test
    void testDetectTabDelimiter() throws IOException {
        String csvContent = "Name\tAge\tCity\n" +
                           "John\t30\tNew York\n" +
                           "Jane\t25\tLos Angeles\n" +
                           "Bob\t35\tChicago\n";
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals('\t', delimiter, "Should detect tab delimiter");
    }

    @Test
    void testDetectPipeDelimiter() throws IOException {
        String csvContent = """
            Name|Age|City
            John|30|New York
            Jane|25|Los Angeles
            Bob|35|Chicago
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals('|', delimiter, "Should detect pipe delimiter");
    }

    @Test
    void testDetectDelimiterWithQuotedFields() throws IOException {
        String csvContent = """
            Name,Age,Address
            "Smith, John",30,"123 Main St, Apt 4"
            "Doe, Jane",25,"456 Oak Ave, Suite 2"
            "Brown, Bob",35,"789 Pine Rd, Unit 1"
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals(',', delimiter, "Should detect comma delimiter even with quoted commas");
    }

    @Test
    void testDetectDelimiterWithInconsistentData() throws IOException {
        String csvContent = """
            Name,Age,City
            John,30,New York
            Jane,25
            Bob,35,Chicago,Extra
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals(',', delimiter, "Should detect comma delimiter even with inconsistent columns");
    }

    @Test
    void testGetDelimiterName() {
        assertEquals("Comma (,)", DelimiterDetector.getDelimiterName(','));
        assertEquals("Semicolon (;)", DelimiterDetector.getDelimiterName(';'));
        assertEquals("Tab (\\t)", DelimiterDetector.getDelimiterName('\t'));
        assertEquals("Pipe (|)", DelimiterDetector.getDelimiterName('|'));
        assertEquals("Unknown (#)", DelimiterDetector.getDelimiterName('#'));
    }

    @Test
    void testEmptyFile() throws IOException {
        String csvContent = "";
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        // Should default to comma when no data
        assertEquals(',', delimiter, "Should default to comma for empty file");
    }

    @Test
    void testSingleLineFile() throws IOException {
        String csvContent = "Name,Age,City\n";
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        char delimiter = DelimiterDetector.detectDelimiter(inputStream);
        
        assertEquals(',', delimiter, "Should detect delimiter from single line");
    }
}

// Made with Bob
