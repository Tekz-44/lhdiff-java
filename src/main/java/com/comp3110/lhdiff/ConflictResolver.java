package com.comp3110.lhdiff;

import org.apache.commons.text.similarity.LevenshteinDistance;
import java.util.*;

/**
 * Step 4: Conflict Resolution
 * Uses detailed textual similarity to pick the best match from candidates
 */
public class ConflictResolver {
    
    private final double threshold;
    private final double contentWeight = 0.6;
    private final double contextWeight = 0.4;
    private static final int CONTEXT_WINDOW = 4;
    
    private final LevenshteinDistance levenshtein;
    
    public ConflictResolver() {
        this(0.5); // Default threshold: 50% similarity required
    }
    
    public ConflictResolver(double threshold) {
        this.threshold = threshold;
        this.levenshtein = new LevenshteinDistance();
    }
    
    /**
     * Calculate normalized Levenshtein similarity [0, 1]
     * 1 = identical, 0 = completely different
     */
    public double levenshteinSimilarity(String str1, String str2) {
        if (str1.isEmpty() && str2.isEmpty()) {
            return 1.0;
        }
        
        if (str1.isEmpty() || str2.isEmpty()) {
            return 0.0;
        }
        
        int maxLen = Math.max(str1.length(), str2.length());
        int distance = levenshtein.apply(str1, str2);
        double similarity = 1.0 - ((double) distance / maxLen);
        
        return Math.max(0.0, similarity);
    }
    
    /**
     * Calculate cosine similarity between two texts
     * Uses word-based vectors
     */
    public double cosineSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(SimHash.tokenize(text1));
        Set<String> words2 = new HashSet<>(SimHash.tokenize(text2));
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        // Calculate intersection
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        if (intersection.isEmpty()) {
            return 0.0;
        }
        
        // Simple cosine calculation
        double numerator = intersection.size();
        double denominator = Math.sqrt(words1.size()) * Math.sqrt(words2.size());
        
        return denominator > 0 ? numerator / denominator : 0.0;
    }
    
    /**
     * Get context text around a line
     */
    public String getContextText(List<String> lines, int lineIdx, int window) {
        int start = Math.max(0, lineIdx - window);
        int end = Math.min(lines.size(), lineIdx + window + 1);
        
        StringBuilder context = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i != lineIdx) {
                if (context.length() > 0) {
                    context.append(" ");
                }
                context.append(lines.get(i));
            }
        }
        
        return context.toString();
    }
    
    /**
     * Compute combined similarity using content and context
     */
    public double computeCombinedSimilarity(String oldLine, String newLine,
                                           String oldContext, String newContext) {
        // Content similarity using Levenshtein
        double contentSim = levenshteinSimilarity(oldLine, newLine);
        
        // Context similarity using cosine
        double contextSim = cosineSimilarity(oldContext, newContext);
        
        // Weighted combination (60% content, 40% context)
        return contentWeight * contentSim + contextWeight * contextSim;
    }
    
    /**
     * Resolve candidate mappings by computing detailed textual similarity
     * 
     * @param oldLines Lines from old file (0-indexed)
     * @param newLines Lines from new file (0-indexed)
     * @param candidates Map of old_line_num -> [candidate_new_line_nums]
     * @return Map of old_line_num -> best_new_line_num
     */
    public Map<Integer, Integer> resolveMappings(List<String> oldLines, List<String> newLines,
                                                Map<Integer, List<Integer>> candidates) {
        Map<Integer, Integer> mappings = new HashMap<>();
        Set<Integer> usedNewLines = new HashSet<>();
        
        // Sort old line numbers for consistent processing
        List<Integer> sortedOldLines = new ArrayList<>(candidates.keySet());
        Collections.sort(sortedOldLines);
        
        // Process each old line and its candidates
        for (int oldLineNum : sortedOldLines) {
            int oldIdx = oldLineNum - 1; // Convert to 0-indexed
            
            if (oldIdx < 0 || oldIdx >= oldLines.size()) {
                continue;
            }
            
            String oldLine = oldLines.get(oldIdx);
            String oldContext = getContextText(oldLines, oldIdx, CONTEXT_WINDOW);
            
            Integer bestMatch = null;
            double bestSimilarity = -1.0;
            
            // Evaluate each candidate
            List<Integer> candidateList = candidates.get(oldLineNum);
            if (candidateList != null) {
                for (int newLineNum : candidateList) {
                    // Skip if already used
                    if (usedNewLines.contains(newLineNum)) {
                        continue;
                    }
                    
                    int newIdx = newLineNum - 1;
                    
                    if (newIdx < 0 || newIdx >= newLines.size()) {
                        continue;
                    }
                    
                    String newLine = newLines.get(newIdx);
                    String newContext = getContextText(newLines, newIdx, CONTEXT_WINDOW);
                    
                    // Compute similarity
                    double similarity = computeCombinedSimilarity(
                        oldLine, newLine, oldContext, newContext
                    );
                    
                    // Track best match
                    if (similarity > bestSimilarity && similarity >= threshold) {
                        bestSimilarity = similarity;
                        bestMatch = newLineNum;
                    }
                }
            }
            
            // Add mapping if found
            if (bestMatch != null) {
                mappings.put(oldLineNum, bestMatch);
                usedNewLines.add(bestMatch);
            }
        }
        
        return mappings;
    }
    
    // Test method
    public static void main(String[] args) {
        ConflictResolver resolver = new ConflictResolver(0.3); // Lower threshold for testing
        
        // Test similarity calculations
        String line1 = "public void hello()";
        String line2 = "public void helloWorld()";
        String line3 = "int x = 5";
        
        System.out.println("Levenshtein Similarity:");
        System.out.println("  '" + line1 + "' vs '" + line2 + "': " + 
                          String.format("%.2f", resolver.levenshteinSimilarity(line1, line2)));
        System.out.println("  '" + line1 + "' vs '" + line3 + "': " + 
                          String.format("%.2f", resolver.levenshteinSimilarity(line1, line3)));
        System.out.println();
        
        // Test with file lines
        List<String> oldLines = Arrays.asList(
            "public class Test {",
            "    public void method1() {",
            "        int x = 5;",
            "    }",
            "}"
        );
        
        List<String> newLines = Arrays.asList(
            "public class Test {",
            "    public void method1Modified() {",
            "        int x = 10;",
            "    }",
            "}"
        );
        
        // Create candidates (line 2 and 3 changed)
        Map<Integer, List<Integer>> candidates = new HashMap<>();
        candidates.put(2, Arrays.asList(2, 3));  // Old line 2 -> candidates 2, 3
        candidates.put(3, Arrays.asList(2, 3));  // Old line 3 -> candidates 2, 3
        
        Map<Integer, Integer> mappings = resolver.resolveMappings(oldLines, newLines, candidates);
        
        System.out.println("Resolved mappings:");
        for (Map.Entry<Integer, Integer> entry : mappings.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println();
        System.out.println("Line 2 should map to 2 (method lines are similar)");
        System.out.println("Line 3 should map to 3 (both are variable declarations)");
    }
}