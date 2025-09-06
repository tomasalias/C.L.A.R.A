package AC.AI.Models;

import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * Represents the analysis result from Gemini AI for player behavior assessment.
 */
@Data
@Builder
public class AIAnalysisResult {
    
    // Analysis metadata
    private String playerId;
    private String playerName;
    private long analysisTimestamp;
    private long responseTimeMs;
    
    // Overall assessment
    private boolean isSuspicious;
    private double confidenceScore; // 0.0 to 1.0
    private CheatLikelihood cheatLikelihood;
    private String recommendation; // "kick", "ban", "warn", "monitor", "clear"
    
    // Detailed analysis
    private List<ViolationAnalysis> violationAnalyses;
    private String reasoningSummary;
    private List<String> suspiciousPatterns;
    private List<String> legitimateBehaviors;
    
    // Action recommendations
    private boolean shouldTakeImmedateAction;
    private String recommendedAction;
    private String actionReason;
    
    @Data
    @Builder
    public static class ViolationAnalysis {
        private String violationType; // "speed", "flight", "reach", "timer"
        private boolean isSuspicious;
        private double suspicionLevel; // 0.0 to 1.0
        private String analysis;
        private String pattern; // "consistent", "burst", "gradual", "random"
        private boolean isLikelyFalsePositive;
        private String falsePositiveReason;
    }
    
    public enum CheatLikelihood {
        VERY_LOW(0.0, 0.2, "Very Low"),
        LOW(0.2, 0.4, "Low"), 
        MODERATE(0.4, 0.6, "Moderate"),
        HIGH(0.6, 0.8, "High"),
        VERY_HIGH(0.8, 1.0, "Very High");
        
        private final double minScore;
        private final double maxScore;
        private final String displayName;
        
        CheatLikelihood(double minScore, double maxScore, String displayName) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.displayName = displayName;
        }
        
        public static CheatLikelihood fromConfidenceScore(double score) {
            for (CheatLikelihood likelihood : values()) {
                if (score >= likelihood.minScore && score < likelihood.maxScore) {
                    return likelihood;
                }
            }
            return score >= 0.8 ? VERY_HIGH : VERY_LOW;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Determines if action should be taken based on the analysis
     */
    public boolean shouldTakeAction(double actionThreshold) {
        return isSuspicious && confidenceScore >= actionThreshold;
    }
    
    /**
     * Gets a human-readable summary of the analysis
     */
    public String getSummary() {
        return String.format("Player: %s | Suspicious: %s | Confidence: %.2f | Likelihood: %s | Action: %s",
            playerName, isSuspicious ? "YES" : "NO", confidenceScore, 
            cheatLikelihood.getDisplayName(), recommendation);
    }
}