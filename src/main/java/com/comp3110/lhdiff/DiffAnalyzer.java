package com.comp3110.lhdiff;

import java.util.*;

/**
 * Step 2: DiffAnalyzer
 * Finds unchanged lines between two file versions using LCS (Longest Common Subsequence)
 */
public class DiffAnalyzer {
    
    /**
     * Find unchanged lines between old and new file versions
     * 
     * @param oldLines Normalized lines from old file
     * @param newLines Normalized lines from new file
     * @return DiffResult containing mappings and changed line sets
     */
    public DiffResult findUnchangedLines(List<String> oldLines, List<String> newLines) {
        Map<Integer, Integer> unchangedMappings = new HashMap<>();
        Set<Integer> oldChanged = new HashSet<>();
        Set<Integer> newChanged = new HashSet<>();
        
        // Initially assume all lines changed
        for (int i = 1; i <= oldLines.size(); i++) {
            oldChanged.add(i);
        }
        for (int i = 1; i <= newLines.size(); i++) {
            newChanged.add(i);
        }
        
        // Find matching blocks using LCS
        List<MatchBlock> matchingBlocks = findMatchingBlocks(oldLines, newLines);
        
        // Map unchanged lines (convert to 1-indexed)
        for (MatchBlock block : matchingBlocks) {
            for (int i = 0; i < block.size; i++) {
                int oldLineNum = block.oldStart + i + 1; // Convert to 1-indexed
                int newLineNum = block.newStart + i + 1;
                
                unchangedMappings.put(oldLineNum, newLineNum);
                oldChanged.remove(oldLineNum);
                newChanged.remove(newLineNum);
            }
        }
        
        return new DiffResult(unchangedMappings, oldChanged, newChanged);
    }
    
    /**
     * Find matching blocks using Longest Common Subsequence algorithm
     */
    private List<MatchBlock> findMatchingBlocks(List<String> oldLines, List<String> newLines) {
        int m = oldLines.size();
        int n = newLines.size();
        
        // Build LCS table
        int[][] lcs = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        
        // Extract matching blocks
        List<MatchBlock> blocks = new ArrayList<>();
        extractMatchingBlocks(oldLines, newLines, lcs, m, n, blocks);
        
        // Merge consecutive blocks
        return mergeConsecutiveBlocks(blocks);
    }
    
    /**
     * Extract matching blocks by backtracking through LCS table
     */
    private void extractMatchingBlocks(List<String> oldLines, List<String> newLines,
                                      int[][] lcs, int i, int j, List<MatchBlock> blocks) {
        if (i == 0 || j == 0) {
            return;
        }
        
        if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
            extractMatchingBlocks(oldLines, newLines, lcs, i - 1, j - 1, blocks);
            blocks.add(new MatchBlock(i - 1, j - 1, 1)); // 0-indexed
        } else if (lcs[i - 1][j] > lcs[i][j - 1]) {
            extractMatchingBlocks(oldLines, newLines, lcs, i - 1, j, blocks);
        } else {
            extractMatchingBlocks(oldLines, newLines, lcs, i, j - 1, blocks);
        }
    }
    
    /**
     * Merge consecutive matching blocks into larger blocks
     */
    private List<MatchBlock> mergeConsecutiveBlocks(List<MatchBlock> blocks) {
        if (blocks.isEmpty()) {
            return blocks;
        }
        
        List<MatchBlock> merged = new ArrayList<>();
        MatchBlock current = blocks.get(0);
        
        for (int i = 1; i < blocks.size(); i++) {
            MatchBlock next = blocks.get(i);
            
            // Check if blocks are consecutive
            if (current.oldStart + current.size == next.oldStart &&
                current.newStart + current.size == next.newStart) {
                // Merge blocks
                current = new MatchBlock(current.oldStart, current.newStart, 
                                        current.size + next.size);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    /**
     * Represents a matching block in the diff
     */
    private static class MatchBlock {
        int oldStart; // 0-indexed
        int newStart; // 0-indexed
        int size;
        
        MatchBlock(int oldStart, int newStart, int size) {
            this.oldStart = oldStart;
            this.newStart = newStart;
            this.size = size;
        }
    }
    
    /**
     * Result of diff analysis
     */
    public static class DiffResult {
        public final Map<Integer, Integer> unchangedMappings; // old line -> new line
        public final Set<Integer> oldChanged; // Changed lines in old file
        public final Set<Integer> newChanged; // Changed lines in new file
        
        public DiffResult(Map<Integer, Integer> unchangedMappings,
                         Set<Integer> oldChanged, Set<Integer> newChanged) {
            this.unchangedMappings = unchangedMappings;
            this.oldChanged = oldChanged;
            this.newChanged = newChanged;
        }
    }
    
    // Test method
    public static void main(String[] args) {
        DiffAnalyzer analyzer = new DiffAnalyzer();
        
        // Test with simple example
        List<String> oldLines = Arrays.asList(
            "line1",
            "line2",
            "line3",
            "line4"
        );
        
        List<String> newLines = Arrays.asList(
            "line1",
            "modified_line2",
            "line3",
            "line5"
        );
        
        DiffResult result = analyzer.findUnchangedLines(oldLines, newLines);
        
        System.out.println("Unchanged mappings:");
        for (Map.Entry<Integer, Integer> entry : result.unchangedMappings.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        System.out.println("\nChanged in old: " + result.oldChanged);
        System.out.println("Changed in new: " + result.newChanged);
    }
}