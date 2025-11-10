package com.comp3110.lhdiff;

import java.util.*;

/**
 * Step 3: Candidate Generation
 * Uses SimHash to efficiently generate candidate line mappings
 */
public class SimHashGenerator {
    
    private final int kCandidates;
    private static final int CONTEXT_WINDOW = 4; // Lines above/below for context
    private static final int MAX_HASH_DISTANCE = 64; // For 64-bit hash
    
    public SimHashGenerator() {
        this(15); // Default: top 15 candidates
    }
    
    public SimHashGenerator(int kCandidates) {
        this.kCandidates = kCandidates;
    }
    
    /**
     * Get context around a line (surrounding lines for comparison)
     * 
     * @param lines All lines in file
     * @param lineIdx Index of target line (0-indexed)
     * @param window Number of lines above/below
     * @return Context as concatenated string
     */
    public String getContext(List<String> lines, int lineIdx, int window) {
        int start = Math.max(0, lineIdx - window);
        int end = Math.min(lines.size(), lineIdx + window + 1);
        
        StringBuilder context = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i != lineIdx) { // Exclude the line itself
                if (context.length() > 0) {
                    context.append(" ");
                }
                context.append(lines.get(i));
            }
        }
        
        return context.toString();
    }
    
    /**
     * Compute content and context SimHash for each line
     * 
     * @param lines All lines in file (0-indexed)
     * @param lineNumbers Line numbers to process (1-indexed)
     * @return Map of line_number -> LineFeatures
     */
    public Map<Integer, LineFeatures> computeLineFeatures(List<String> lines, Set<Integer> lineNumbers) {
        Map<Integer, LineFeatures> features = new HashMap<>();
        
        for (int lineNum : lineNumbers) {
            int lineIdx = lineNum - 1; // Convert to 0-indexed
            
            if (lineIdx < 0 || lineIdx >= lines.size()) {
                continue;
            }
            
            // Content: the line itself
            String content = lines.get(lineIdx);
            long contentHash = SimHash.compute(SimHash.tokenize(content));
            
            // Context: surrounding lines
            String context = getContext(lines, lineIdx, CONTEXT_WINDOW);
            long contextHash = SimHash.compute(SimHash.tokenize(context));
            
            features.put(lineNum, new LineFeatures(contentHash, contextHash));
        }
        
        return features;
    }
    
    /**
     * Compute combined similarity score from hamming distances
     * 
     * @param contentDist Hamming distance for content
     * @param contextDist Hamming distance for context
     * @return Combined similarity [0, 1]
     */
    public double computeCombinedSimilarity(int contentDist, int contextDist) {
        // Convert distance to similarity
        double contentSim = 1.0 - ((double) contentDist / MAX_HASH_DISTANCE);
        double contextSim = 1.0 - ((double) contextDist / MAX_HASH_DISTANCE);
        
        // Weighted combination (60% content, 40% context)
        return 0.6 * contentSim + 0.4 * contextSim;
    }
    
    /**
     * Generate candidate mappings using SimHash similarity
     * 
     * @param leftFeatures Features for left (old) file lines
     * @param rightFeatures Features for right (new) file lines
     * @return Map of left_line_num -> [list of top-k right_line_nums]
     */
    public Map<Integer, List<Integer>> generateCandidates(Map<Integer, LineFeatures> leftFeatures,
                                                          Map<Integer, LineFeatures> rightFeatures) {
        Map<Integer, List<Integer>> candidates = new HashMap<>();
        
        for (Map.Entry<Integer, LineFeatures> leftEntry : leftFeatures.entrySet()) {
            int leftLine = leftEntry.getKey();
            LineFeatures leftFeat = leftEntry.getValue();
            
            // Calculate similarity with all right lines
            List<CandidateScore> similarities = new ArrayList<>();
            
            for (Map.Entry<Integer, LineFeatures> rightEntry : rightFeatures.entrySet()) {
                int rightLine = rightEntry.getKey();
                LineFeatures rightFeat = rightEntry.getValue();
                
                int contentDist = SimHash.hammingDistance(leftFeat.contentHash, rightFeat.contentHash);
                int contextDist = SimHash.hammingDistance(leftFeat.contextHash, rightFeat.contextHash);
                
                double combinedSim = computeCombinedSimilarity(contentDist, contextDist);
                similarities.add(new CandidateScore(rightLine, combinedSim));
            }
            
            // Sort by similarity (descending) and take top k
            Collections.sort(similarities, (a, b) -> Double.compare(b.score, a.score));
            
            List<Integer> topK = new ArrayList<>();
            for (int i = 0; i < Math.min(kCandidates, similarities.size()); i++) {
                topK.add(similarities.get(i).lineNum);
            }
            
            candidates.put(leftLine, topK);
        }
        
        return candidates;
    }
    
    /**
     * Container for line features (content and context hashes)
     */
    public static class LineFeatures {
        public final long contentHash;
        public final long contextHash;
        
        public LineFeatures(long contentHash, long contextHash) {
            this.contentHash = contentHash;
            this.contextHash = contextHash;
        }
    }
    
    /**
     * Helper class for sorting candidates by score
     */
    private static class CandidateScore {
        final int lineNum;
        final double score;
        
        CandidateScore(int lineNum, double score) {
            this.lineNum = lineNum;
            this.score = score;
        }
    }
    
    // Test method
    public static void main(String[] args) {
        SimHashGenerator generator = new SimHashGenerator(3); // Top 3 candidates
        
        // Test with simple lines
        List<String> lines = Arrays.asList(
            "public void method1()",
            "public void method2()",
            "int x = 5",
            "int y = 10"
        );
        
        // Features for line 1
        Set<Integer> leftLines = new HashSet<>(Arrays.asList(1));
        Set<Integer> rightLines = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        
        Map<Integer, LineFeatures> leftFeatures = generator.computeLineFeatures(lines, leftLines);
        Map<Integer, LineFeatures> rightFeatures = generator.computeLineFeatures(lines, rightLines);
        
        Map<Integer, List<Integer>> candidates = generator.generateCandidates(leftFeatures, rightFeatures);
        
        System.out.println("Candidate mappings (top 3 for each line):");
        for (Map.Entry<Integer, List<Integer>> entry : candidates.entrySet()) {
            System.out.println("  Line " + entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println();
        System.out.println("Line 1 should match most closely with lines 1 and 2 (similar structure)");
    }
}