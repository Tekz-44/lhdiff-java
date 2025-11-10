package com.comp3110.lhdiff;

import com.comp3110.lhdiff.DiffAnalyzer.DiffResult;
import com.comp3110.lhdiff.Preprocessor.FileLines;
import com.comp3110.lhdiff.SimHashGenerator.LineFeatures;

import java.io.IOException;
import java.util.*;

/**
 * Main LHDiff Module
 * Integrates all 5 steps into a complete line mapping pipeline
 */
public class LHDiff {
    
    private final Preprocessor preprocessor;
    private final DiffAnalyzer diffAnalyzer;
    private final SimHashGenerator simHashGenerator;
    private final ConflictResolver conflictResolver;
    private final LineSplitDetector splitDetector;
    
    public LHDiff() {
        this(15, 0.5, 0.05);
    }
    
    public LHDiff(int kCandidates, double similarityThreshold, double splitThreshold) {
        this.preprocessor = new Preprocessor();
        this.diffAnalyzer = new DiffAnalyzer();
        this.simHashGenerator = new SimHashGenerator(kCandidates);
        this.conflictResolver = new ConflictResolver(similarityThreshold);
        this.splitDetector = new LineSplitDetector(splitThreshold);
    }
    
    /**
     * Main pipeline to map lines between two file versions
     */
    public MappingResult mapLines(String oldFile, String newFile, boolean verbose) throws IOException {
        if (verbose) {
            System.out.println("============================================================");
            System.out.println("LHDiff: Line Mapping Pipeline");
            System.out.println("============================================================");
        }
        
        // Step 1: Preprocessing
        if (verbose) {
            System.out.println("\nStep 1: Preprocessing...");
        }
        
        FileLines oldFileLines = preprocessor.preprocessFile(oldFile);
        FileLines newFileLines = preprocessor.preprocessFile(newFile);
        
        if (verbose) {
            System.out.println("  Old file: " + oldFileLines.size() + " lines");
            System.out.println("  New file: " + newFileLines.size() + " lines");
        }
        
        // Step 2: Detect unchanged lines
        if (verbose) {
            System.out.println("\nStep 2: Detecting unchanged lines...");
        }
        
        DiffResult diffResult = diffAnalyzer.findUnchangedLines(
            oldFileLines.getNormalizedLines(),
            newFileLines.getNormalizedLines()
        );
        
        if (verbose) {
            System.out.println("  Unchanged: " + diffResult.unchangedMappings.size() + " lines");
            System.out.println("  Changed in old: " + diffResult.oldChanged.size() + " lines");
            System.out.println("  Changed in new: " + diffResult.newChanged.size() + " lines");
        }
        
        // Step 3: Generate candidates using SimHash
        if (verbose) {
            System.out.println("\nStep 3: Generating candidates with SimHash...");
        }
        
        Map<Integer, LineFeatures> leftFeatures = simHashGenerator.computeLineFeatures(
            oldFileLines.getOriginalLines(),
            diffResult.oldChanged
        );
        
        Map<Integer, LineFeatures> rightFeatures = simHashGenerator.computeLineFeatures(
            newFileLines.getOriginalLines(),
            diffResult.newChanged
        );
        
        Map<Integer, List<Integer>> candidates = simHashGenerator.generateCandidates(
            leftFeatures, rightFeatures
        );
        
        if (verbose) {
            System.out.println("  Generated candidates for " + candidates.size() + " lines");
        }
        
        // Step 4: Resolve conflicts
        if (verbose) {
            System.out.println("\nStep 4: Resolving conflicts...");
        }
        
        Map<Integer, Integer> resolvedMappings = conflictResolver.resolveMappings(
            oldFileLines.getOriginalLines(),
            newFileLines.getOriginalLines(),
            candidates
        );
        
        if (verbose) {
            System.out.println("  Resolved: " + resolvedMappings.size() + " mappings");
        }
        
        // Combine unchanged and resolved mappings
        Map<Integer, Integer> allMappings = new HashMap<>(diffResult.unchangedMappings);
        allMappings.putAll(resolvedMappings);
        
        // Step 5: Detect line splits
        if (verbose) {
            System.out.println("\nStep 5: Detecting line splits...");
        }
        
        // Find still unmapped lines
        Set<Integer> unmappedOld = new HashSet<>(diffResult.oldChanged);
        unmappedOld.removeAll(resolvedMappings.keySet());
        
        Set<Integer> unmappedNew = new HashSet<>(diffResult.newChanged);
        unmappedNew.removeAll(resolvedMappings.values());
        
        Map<Integer, List<Integer>> splitMappings = splitDetector.detectSplits(
            oldFileLines.getOriginalLines(),
            newFileLines.getOriginalLines(),
            unmappedOld,
            unmappedNew
        );
        
        if (verbose) {
            System.out.println("  Detected: " + splitMappings.size() + " line splits");
        }
        
        // Update unmapped sets
        unmappedOld.removeAll(splitMappings.keySet());
        for (List<Integer> splits : splitMappings.values()) {
            unmappedNew.removeAll(splits);
        }
        
        if (verbose) {
            System.out.println("\n============================================================");
            System.out.println("Mapping Complete!");
            System.out.println("============================================================");
        }
        
        return new MappingResult(
            allMappings,
            splitMappings,
            diffResult.unchangedMappings,
            resolvedMappings,
            unmappedOld,
            unmappedNew
        );
    }
    
    /**
     * Format results in required output format
     */
    public String formatOutput(MappingResult result) {
        StringBuilder output = new StringBuilder();
        
        // Sort by old line number and format 1-to-1 mappings
        List<Integer> sortedKeys = new ArrayList<>(result.mappings.keySet());
        Collections.sort(sortedKeys);
        
        for (int oldLine : sortedKeys) {
            int newLine = result.mappings.get(oldLine);
            output.append(oldLine).append("-").append(newLine).append("\n");
        }
        
        // Add split mappings
        List<Integer> sortedSplitKeys = new ArrayList<>(result.splits.keySet());
        Collections.sort(sortedSplitKeys);
        
        for (int oldLine : sortedSplitKeys) {
            List<Integer> newLines = result.splits.get(oldLine);
            output.append(oldLine).append("-[");
            for (int i = 0; i < newLines.size(); i++) {
                if (i > 0) output.append(",");
                output.append(newLines.get(i));
            }
            output.append("]\n");
        }
        
        return output.toString();
    }
    
    /**
     * Print detailed results
     */
    public void printDetailedResults(MappingResult result) {
        System.out.println("\n============================================================");
        System.out.println("DETAILED RESULTS");
        System.out.println("============================================================");
        
        System.out.println("\n1-to-1 Mappings:");
        List<Integer> sortedMappings = new ArrayList<>(result.mappings.keySet());
        Collections.sort(sortedMappings);
        for (int oldLine : sortedMappings) {
            System.out.println("  " + oldLine + " -> " + result.mappings.get(oldLine));
        }
        
        if (!result.splits.isEmpty()) {
            System.out.println("\n1-to-Many Mappings (Line Splits):");
            List<Integer> sortedSplits = new ArrayList<>(result.splits.keySet());
            Collections.sort(sortedSplits);
            for (int oldLine : sortedSplits) {
                System.out.println("  " + oldLine + " -> " + result.splits.get(oldLine));
            }
        }
        
        if (!result.unmappedOld.isEmpty()) {
            System.out.println("\nUnmapped Old Lines:");
            List<Integer> sortedUnmappedOld = new ArrayList<>(result.unmappedOld);
            Collections.sort(sortedUnmappedOld);
            System.out.println("  " + sortedUnmappedOld);
        }
        
        if (!result.unmappedNew.isEmpty()) {
            System.out.println("\nUnmapped New Lines:");
            List<Integer> sortedUnmappedNew = new ArrayList<>(result.unmappedNew);
            Collections.sort(sortedUnmappedNew);
            System.out.println("  " + sortedUnmappedNew);
        }
        
        System.out.println("\nSummary:");
        System.out.println("  Total mappings: " + result.mappings.size());
        System.out.println("  Split mappings: " + result.splits.size());
        System.out.println("  Unmapped old: " + result.unmappedOld.size());
        System.out.println("  Unmapped new: " + result.unmappedNew.size());
    }
    
    /**
     * Result container
     */
    public static class MappingResult {
        public final Map<Integer, Integer> mappings;
        public final Map<Integer, List<Integer>> splits;
        public final Map<Integer, Integer> unchanged;
        public final Map<Integer, Integer> resolved;
        public final Set<Integer> unmappedOld;
        public final Set<Integer> unmappedNew;
        
        public MappingResult(Map<Integer, Integer> mappings,
                           Map<Integer, List<Integer>> splits,
                           Map<Integer, Integer> unchanged,
                           Map<Integer, Integer> resolved,
                           Set<Integer> unmappedOld,
                           Set<Integer> unmappedNew) {
            this.mappings = mappings;
            this.splits = splits;
            this.unchanged = unchanged;
            this.resolved = resolved;
            this.unmappedOld = unmappedOld;
            this.unmappedNew = unmappedNew;
        }
    }
    
    /**
     * Main method - Command line interface
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java LHDiff <old_file> <new_file> [--verbose]");
            System.out.println("\nExample:");
            System.out.println("  java LHDiff version1.java version2.java");
            System.out.println("  java LHDiff version1.java version2.java --verbose");
            return;
        }
        
        String oldFile = args[0];
        String newFile = args[1];
        boolean verbose = args.length > 2 && (args[2].equals("--verbose") || args[2].equals("-v"));
        
        try {
            LHDiff lhdiff = new LHDiff();
            MappingResult result = lhdiff.mapLines(oldFile, newFile, verbose);
            
            System.out.println("\n============================================================");
            System.out.println("LINE MAPPINGS");
            System.out.println("============================================================");
            System.out.print(lhdiff.formatOutput(result));
            
            if (verbose) {
                lhdiff.printDetailedResults(result);
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}