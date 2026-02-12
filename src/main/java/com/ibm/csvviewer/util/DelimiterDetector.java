package com.ibm.csvviewer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to automatically detect CSV delimiter
 */
public class DelimiterDetector {
    
    private static final char[] COMMON_DELIMITERS = {',', ';', '\t', '|'};
    private static final int SAMPLE_LINES = 10;

    /**
     * Detect the delimiter used in a CSV file
     * 
     * @param inputStream Input stream of the CSV file
     * @return Detected delimiter character
     * @throws IOException If reading fails
     */
    public static char detectDelimiter(InputStream inputStream) throws IOException {
        Map<Character, Integer> delimiterCounts = new HashMap<>();
        Map<Character, Integer> consistencyScores = new HashMap<>();
        
        for (char delimiter : COMMON_DELIMITERS) {
            delimiterCounts.put(delimiter, 0);
            consistencyScores.put(delimiter, 0);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            int lineCount = 0;
            Map<Character, Integer> previousCounts = new HashMap<>();
            
            while ((line = reader.readLine()) != null && lineCount < SAMPLE_LINES) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                Map<Character, Integer> currentCounts = new HashMap<>();
                
                for (char delimiter : COMMON_DELIMITERS) {
                    int count = countOccurrences(line, delimiter);
                    currentCounts.put(delimiter, count);
                    delimiterCounts.put(delimiter, delimiterCounts.get(delimiter) + count);
                    
                    // Check consistency with previous line
                    if (lineCount > 0 && previousCounts.containsKey(delimiter)) {
                        if (count == previousCounts.get(delimiter) && count > 0) {
                            consistencyScores.put(delimiter, 
                                consistencyScores.get(delimiter) + 1);
                        }
                    }
                }
                
                previousCounts = currentCounts;
                lineCount++;
            }
        }

        // Find delimiter with best consistency score
        char bestDelimiter = ',';
        int bestScore = -1;
        
        for (char delimiter : COMMON_DELIMITERS) {
            int score = consistencyScores.get(delimiter);
            int totalCount = delimiterCounts.get(delimiter);
            
            // Prefer delimiters that appear consistently and frequently
            if (score > bestScore && totalCount > 0) {
                bestScore = score;
                bestDelimiter = delimiter;
            }
        }
        
        // If no consistent delimiter found, use the most frequent one
        if (bestScore == 0) {
            int maxCount = 0;
            for (char delimiter : COMMON_DELIMITERS) {
                int count = delimiterCounts.get(delimiter);
                if (count > maxCount) {
                    maxCount = count;
                    bestDelimiter = delimiter;
                }
            }
        }
        
        return bestDelimiter;
    }

    /**
     * Count occurrences of a character in a string, ignoring quoted sections
     */
    private static int countOccurrences(String line, char delimiter) {
        int count = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Get delimiter name for display
     */
    public static String getDelimiterName(char delimiter) {
        return switch (delimiter) {
            case ',' -> "Comma (,)";
            case ';' -> "Semicolon (;)";
            case '\t' -> "Tab (\\t)";
            case '|' -> "Pipe (|)";
            default -> "Unknown (" + delimiter + ")";
        };
    }
}

// Made with Bob
