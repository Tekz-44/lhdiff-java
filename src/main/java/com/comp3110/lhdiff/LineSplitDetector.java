package com.comp3110.lhdiff;

import org.apache.commons.text.similarity.LevenshteinDistance;
import java.util.*;

/**
 * Step 5: Line Split Detection
 * Detects when a single line in old file maps to multiple lines in new file
 */
public class LineSplitDetector {
    
    private final double improvementThreshold;
    private final LevenshteinDistance levenshtein;
    
    public LineSplitDetector() {
        this(0.05); // Default: require 5% improvement to add another line
    }
    
    public LineSplitDetector(double improvementThreshold) {
        this.improvementThreshold = improvementThreshold;
        this.levenshtein = new LevenshteinDistance();
    }
    
    /**
     * Calculate normalized Levenshtein distance [0, 1]
     * 0 = identical, 1 = completely different
     */
    public double normalizedLevenshteinDistance(String str1, String str2) {
        if (str1.isEmpty() && str2.isEmpty()) {
            return 0.0;
        }
        
        if (str1.isEmpty() || str2.isEmpty()) {
            return 1.0;
        }
        
        int maxLen = Math.max(str1.length(), str2.length());
        int distance = levenshtein.apply(str1, str2);
        
        return (double) distance / maxLen;
    }
    
    /**
     * Concatenate multiple lines into one string
     */
    public String concatenateLines(List<String> lines, List<Integer> indices) {
        StringBuilder result = new StringBuilder();
        
        for (int idx : indices) {
            int lineIdx = idx - 1; // Convert to 0-indexed
            if (lineIdx >= 0 && lineIdx < lines.size()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(lines.get(lineIdx));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Detect line splits for unmapped lines in old file
     * 
     * @param oldLines Lines from old file (0-indexed)
     * @param newLines Lines from new file (0-indexed)
     * @param unmappedOld Set of unmapped line numbers from old file (1-indexed)
     * @param unmappedNew Set of unmapped line numbers from new file (1-indexed)
     * @return Map of old_line_num -> [list of new_line_nums]
     */
    public Map<Integer, List<Integer>> detectSplits(List<String> oldLines, List<String> newLines,
                                                    Set<Integer> unmappedOld, Set<Integer> unmappedNew) {
        Map<Integer, List<Integer>> splitMappings = new HashMap<>();
        Set<Integer> usedNewLines = new HashSet<>();
        
        // Convert to sorted lists
        List<Integer> unmappedOldList = new ArrayList<>(unmappedOld);
        List<Integer> unmappedNewList = new ArrayList<>(unmappedNew);
        Collections.sort(unmappedOldList);
        Collections.sort(unmappedNewList);
        
        for (int oldLineNum : unmappedOldList) {
            int oldIdx = oldLineNum - 1;
            
            if (oldIdx < 0 || oldIdx >= oldLines.size()) {
                continue;
            }
            
            String oldLine = oldLines.get(oldIdx);
            
            // Try to find consecutive new lines that combine to match old line
            List<Integer> bestSplit = findBestSplit(
                oldLine, newLines, unmappedNewList, usedNewLines
            );
            
            // Only record actual splits (2+ lines)
            if (bestSplit != null && bestSplit.size() > 1) {
                splitMappings.put(oldLineNum, bestSplit);
                usedNewLines.addAll(bestSplit);
            }
        }
        
        return splitMappings;
    }
    
    /**
     * Find the best sequence of new lines that matches an old line
     */
    private List<Integer> findBestSplit(String oldLine, List<String> newLines,
                                       List<Integer> unmappedNew, Set<Integer> usedNewLines) {
        List<Integer> bestSplit = new ArrayList<>();
        
        // Try starting from each unmapped new line
        for (int startIdx = 0; startIdx < unmappedNew.size(); startIdx++) {
            int startLineNum = unmappedNew.get(startIdx);
            
            if (usedNewLines.contains(startLineNum)) {
                continue;
            }
            
            // Build sequence starting from this line
            List<Integer> sequence = new ArrayList<>();
            sequence.add(startLineNum);
            
            double prevDistance = normalizedLevenshteinDistance(
                oldLine,
                concatenateLines(newLines, sequence)
            );
            
            // Keep adding consecutive lines while similarity improves
            for (int nextIdx = startIdx + 1; nextIdx < unmappedNew.size(); nextIdx++) {
                int nextLineNum = unmappedNew.get(nextIdx);
                
                if (usedNewLines.contains(nextLineNum)) {
                    break;
                }
                
                // Check if this line is consecutive
                if (nextLineNum != sequence.get(sequence.size() - 1) + 1) {
                    break;
                }
                
                // Try adding this line
                List<Integer> testSequence = new ArrayList<>(sequence);
                testSequence.add(nextLineNum);
                
                String combinedText = concatenateLines(newLines, testSequence);
                double newDistance = normalizedLevenshteinDistance(oldLine, combinedText);
                
                // Check if similarity improved
                double improvement = prevDistance - newDistance;
                
                if (improvement >= improvementThreshold) {
                    sequence.add(nextLineNum);
                    prevDistance = newDistance;
                } else {
                    break; // Stop if no improvement
                }
            }
            
            // Keep track of best split found
            if (sequence.size() > bestSplit.size()) {
                bestSplit = new ArrayList<>(sequence);
            }
        }
        
        return bestSplit;
    }
    
    // Test method
    public static void main(String[] args) {
        LineSplitDetector detector = new LineSplitDetector();
        
        // Test case: one line split into three
        List<String> oldLines = Arrays.asList(
            "public void method() { int x = 5; return x; }"
        );
        
        List<String> newLines = Arrays.asList(
            "public void method() {",
            "    int x = 5;",
            "    return x;",
            "}"
        );
        
        Set<Integer> unmappedOld = new HashSet<>(Arrays.asList(1));
        Set<Integer> unmappedNew = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        
        Map<Integer, List<Integer>> splits = detector.detectSplits(
            oldLines, newLines, unmappedOld, unmappedNew
        );
        
        System.out.println("Detected line splits:");
        for (Map.Entry<Integer, List<Integer>> entry : splits.entrySet()) {
            System.out.println("  Old line " + entry.getKey() + " -> New lines " + entry.getValue());
        }
        System.out.println();
        
        if (splits.containsKey(1)) {
            System.out.println("✓ Successfully detected that old line 1 was split into multiple new lines!");
        } else {
            System.out.println("Note: Split detection may vary based on thresholds.");
        }
        
        // Test distance calculation
        System.out.println("\nDistance examples:");
        String str1 = "hello world";
        String str2 = "hello";
        double dist = detector.normalizedLevenshteinDistance(str1, str2);
        System.out.println("  Distance between '" + str1 + "' and '" + str2 + "': " + 
                          String.format("%.2f", dist));
    }
}