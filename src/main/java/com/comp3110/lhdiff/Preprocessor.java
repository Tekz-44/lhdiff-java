package com.comp3110.lhdiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 1: Preprocessor
 * Normalizes source code lines by removing extra whitespace and comments
 */
public class Preprocessor {
    
    /**
     * Normalize a single line:
     * - Remove leading/trailing spaces
     * - Replace multiple spaces with single space
     * - Remove simple comments
     */
    public String normalizeLine(String line) {
        // Remove leading and trailing whitespace
        String normalized = line.trim();
        
        // Replace multiple spaces with single space
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Remove common single-line comments
        normalized = normalized.replaceAll("//.*$", ""); // Remove // comments
        normalized = normalized.replaceAll("#.*$", "");  // Remove # comments
        normalized = normalized.trim();
        
        return normalized;
    }
    
    /**
     * Read a file and return both original and normalized lines
     */
    public FileLines preprocessFile(String filepath) throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(filepath));
        
        List<String> originalLines = new ArrayList<>();
        List<String> normalizedLines = new ArrayList<>();
        
        for (String line : allLines) {
            originalLines.add(line);
            normalizedLines.add(normalizeLine(line));
        }
        
        return new FileLines(originalLines, normalizedLines);
    }
    
    /**
     * Container class to hold both original and normalized lines
     */
    public static class FileLines {
        private final List<String> originalLines;
        private final List<String> normalizedLines;
        
        public FileLines(List<String> originalLines, List<String> normalizedLines) {
            this.originalLines = originalLines;
            this.normalizedLines = normalizedLines;
        }
        
        public List<String> getOriginalLines() {
            return originalLines;
        }
        
        public List<String> getNormalizedLines() {
            return normalizedLines;
        }
        
        public int size() {
            return originalLines.size();
        }
    }
    
    // Test method
    public static void main(String[] args) {
        Preprocessor preprocessor = new Preprocessor();
        
        // Test normalization
        String testLine1 = "  public   void   hello()   // comment";
        String testLine2 = "    int x = 5;   # another comment   ";
        
        System.out.println("Original: '" + testLine1 + "'");
        System.out.println("Normalized: '" + preprocessor.normalizeLine(testLine1) + "'");
        System.out.println();
        System.out.println("Original: '" + testLine2 + "'");
        System.out.println("Normalized: '" + preprocessor.normalizeLine(testLine2) + "'");
    }
}