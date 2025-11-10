package com.comp3110.lhdiff;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * SimHash implementation for similarity hashing
 * Used to quickly find similar lines without comparing all pairs
 */
public class SimHash {
    
    private static final int HASH_BITS = 64;
    
    /**
     * Compute SimHash value for a list of tokens (words)
     * 
     * @param tokens List of words
     * @return SimHash value as long
     */
    public static long compute(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return 0L;
        }
        
        int[] vector = new int[HASH_BITS];
        
        // Process each token
        for (String token : tokens) {
            long hash = hashToken(token);
            
            // Update vector based on hash bits
            for (int i = 0; i < HASH_BITS; i++) {
                long bit = (hash >> i) & 1;
                if (bit == 1) {
                    vector[i]++;
                } else {
                    vector[i]--;
                }
            }
        }
        
        // Generate final hash from vector
        long simhash = 0L;
        for (int i = 0; i < HASH_BITS; i++) {
            if (vector[i] > 0) {
                simhash |= (1L << i);
            }
        }
        
        return simhash;
    }
    
    /**
     * Hash a single token using MD5
     */
    private static long hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(token.getBytes());
            
            // Convert first 8 bytes to long
            long hash = 0;
            for (int i = 0; i < Math.min(8, digest.length); i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            // Fallback to hashCode if MD5 not available
            return token.hashCode();
        }
    }
    
    /**
     * Calculate Hamming distance between two hash values
     * (How many bits are different)
     * 
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return Number of differing bits
     */
    public static int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        return Long.bitCount(xor);
    }
    
    /**
     * Tokenize text into words
     * Splits on non-alphanumeric characters and converts to lowercase
     * 
     * @param text Input text
     * @return List of tokens
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        
        // Split on non-alphanumeric characters
        String[] words = text.toLowerCase().split("\\W+");
        
        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }
        
        return tokens;
    }
    
    // Test method
    public static void main(String[] args) {
        // Test tokenization
        String text1 = "public void hello()";
        String text2 = "public void goodbye()";
        String text3 = "int x = 5";
        
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);
        List<String> tokens3 = tokenize(text3);
        
        System.out.println("Text 1 tokens: " + tokens1);
        System.out.println("Text 2 tokens: " + tokens2);
        System.out.println("Text 3 tokens: " + tokens3);
        System.out.println();
        
        // Compute SimHash values
        long hash1 = compute(tokens1);
        long hash2 = compute(tokens2);
        long hash3 = compute(tokens3);
        
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hash 3: " + hash3);
        System.out.println();
        
        // Calculate similarities (lower distance = more similar)
        int dist12 = hammingDistance(hash1, hash2);
        int dist13 = hammingDistance(hash1, hash3);
        int dist23 = hammingDistance(hash2, hash3);
        
        System.out.println("Distance between text1 and text2: " + dist12 + " (similar methods)");
        System.out.println("Distance between text1 and text3: " + dist13 + " (very different)");
        System.out.println("Distance between text2 and text3: " + dist23 + " (very different)");
        System.out.println();
        System.out.println("Lower distance = more similar!");
    }
}