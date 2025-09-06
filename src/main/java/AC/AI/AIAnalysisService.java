package AC.AI;

import AC.AI.Models.AIAnalysisResult;
import AC.AI.Models.PlayerBehaviorData;
import AC.CLARA;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

/**
 * Main AI analysis service for C.L.A.R.A.
 * Orchestrates player behavior analysis using Gemini AI.
 */
public class AIAnalysisService {
    
    @Getter private static AIAnalysisService instance;
    
    @Getter private final AIConfig config;
    @Getter private final GeminiApiClient geminiClient;
    private final PromptEngine promptEngine;
    private final ExecutorService executorService;
    private final Gson gson;
    
    // Cache recent analyses to avoid duplicate processing
    private final ConcurrentHashMap<String, AIAnalysisResult> analysisCache;
    private final ConcurrentHashMap<String, Long> lastAnalysisTime;
    
    public AIAnalysisService(ExecutorService executorService) {
        instance = this;
        
        this.config = new AIConfig();
        this.geminiClient = new GeminiApiClient(config);
        this.promptEngine = new PromptEngine();
        this.executorService = executorService;
        this.analysisCache = new ConcurrentHashMap<>();
        this.lastAnalysisTime = new ConcurrentHashMap<>();
        
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Test API connection if enabled
        if (config.isValidConfiguration()) {
            testConnection();
        }
    }
    
    /**
     * Analyzes player behavior asynchronously
     */
    public CompletableFuture<AIAnalysisResult> analyzePlayerBehavior(PlayerBehaviorData behaviorData) {
        if (!config.isValidConfiguration()) {
            return CompletableFuture.completedFuture(createDisabledResult(behaviorData));
        }
        
        String playerId = behaviorData.getPlayerId();
        
        // Check if we recently analyzed this player
        if (shouldSkipAnalysis(playerId)) {
            AIAnalysisResult cachedResult = analysisCache.get(playerId);
            if (cachedResult != null) {
                return CompletableFuture.completedFuture(cachedResult);
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Create comprehensive analysis prompt
                String prompt = promptEngine.createAnalysisPrompt(behaviorData);
                
                // Send to Gemini AI
                String aiResponse = geminiClient.analyze(prompt);
                
                // Parse AI response
                AIAnalysisResult result = parseAIResponse(aiResponse, behaviorData, startTime);
                
                // Cache result
                analysisCache.put(playerId, result);
                lastAnalysisTime.put(playerId, System.currentTimeMillis());
                
                // Log if configured
                if (config.isLogAiAnalysis()) {
                    logAnalysisResult(result);
                }
                
                return result;
                
            } catch (Exception e) {
                CLARA.getInstance().getLogger().log(Level.WARNING, 
                    "Failed to analyze player behavior for " + behaviorData.getPlayerName(), e);
                    
                return createErrorResult(behaviorData, e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Quick analysis for immediate decisions
     */
    public CompletableFuture<AIAnalysisResult> quickAnalysis(PlayerBehaviorData behaviorData) {
        if (!config.isValidConfiguration()) {
            return CompletableFuture.completedFuture(createDisabledResult(behaviorData));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                String prompt = promptEngine.createQuickAnalysisPrompt(behaviorData);
                String aiResponse = geminiClient.analyze(prompt);
                
                return parseQuickResponse(aiResponse, behaviorData, startTime);
                
            } catch (Exception e) {
                CLARA.getInstance().getLogger().log(Level.WARNING, 
                    "Failed quick analysis for " + behaviorData.getPlayerName(), e);
                return createErrorResult(behaviorData, e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Parses the full AI analysis response
     */
    private AIAnalysisResult parseAIResponse(String aiResponse, PlayerBehaviorData behaviorData, long startTime) {
        try {
            // Extract JSON from response (AI might include additional text)
            String jsonResponse = extractJsonFromResponse(aiResponse);
            
            // Parse the JSON response
            com.google.gson.JsonObject jsonObject = gson.fromJson(jsonResponse, com.google.gson.JsonObject.class);
            
            AIAnalysisResult.AIAnalysisResultBuilder builder = AIAnalysisResult.builder()
                .playerId(behaviorData.getPlayerId())
                .playerName(behaviorData.getPlayerName())
                .analysisTimestamp(System.currentTimeMillis())
                .responseTimeMs(System.currentTimeMillis() - startTime);
                
            // Extract main analysis fields
            if (jsonObject.has("isSuspicious")) {
                builder.isSuspicious(jsonObject.get("isSuspicious").getAsBoolean());
            }
            
            if (jsonObject.has("confidenceScore")) {
                double confidence = jsonObject.get("confidenceScore").getAsDouble();
                builder.confidenceScore(Math.max(0.0, Math.min(1.0, confidence))); // Clamp to [0,1]
                builder.cheatLikelihood(AIAnalysisResult.CheatLikelihood.fromConfidenceScore(confidence));
            }
            
            if (jsonObject.has("recommendation")) {
                builder.recommendation(jsonObject.get("recommendation").getAsString());
            }
            
            if (jsonObject.has("reasoningSummary")) {
                builder.reasoningSummary(jsonObject.get("reasoningSummary").getAsString());
            }
            
            if (jsonObject.has("shouldTakeImmedateAction")) {
                builder.shouldTakeImmedateAction(jsonObject.get("shouldTakeImmedateAction").getAsBoolean());
            }
            
            if (jsonObject.has("recommendedAction")) {
                builder.recommendedAction(jsonObject.get("recommendedAction").getAsString());
            }
            
            if (jsonObject.has("actionReason")) {
                builder.actionReason(jsonObject.get("actionReason").getAsString());
            }
            
            // Extract arrays (simplified for now)
            if (jsonObject.has("suspiciousPatterns")) {
                // Convert JsonArray to List<String> - simplified implementation
                builder.suspiciousPatterns(java.util.Collections.emptyList());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses quick analysis response
     */
    private AIAnalysisResult parseQuickResponse(String aiResponse, PlayerBehaviorData behaviorData, long startTime) {
        try {
            String jsonResponse = extractJsonFromResponse(aiResponse);
            com.google.gson.JsonObject jsonObject = gson.fromJson(jsonResponse, com.google.gson.JsonObject.class);
            
            boolean isSuspicious = jsonObject.has("isSuspicious") ? 
                jsonObject.get("isSuspicious").getAsBoolean() : false;
            double confidence = jsonObject.has("confidence") ? 
                jsonObject.get("confidence").getAsDouble() : 0.0;
            String reason = jsonObject.has("reason") ? 
                jsonObject.get("reason").getAsString() : "No reason provided";
            
            return AIAnalysisResult.builder()
                .playerId(behaviorData.getPlayerId())
                .playerName(behaviorData.getPlayerName())
                .analysisTimestamp(System.currentTimeMillis())
                .responseTimeMs(System.currentTimeMillis() - startTime)
                .isSuspicious(isSuspicious)
                .confidenceScore(confidence)
                .cheatLikelihood(AIAnalysisResult.CheatLikelihood.fromConfidenceScore(confidence))
                .reasoningSummary(reason)
                .recommendation(isSuspicious ? (confidence > 0.8 ? "kick" : "warn") : "clear")
                .build();
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse quick analysis response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts JSON from AI response (handles cases where AI includes extra text)
     */
    private String extractJsonFromResponse(String response) {
        // Find JSON block markers
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        // If no JSON markers found, assume entire response is JSON
        return response.trim();
    }
    
    /**
     * Creates a result for when AI is disabled
     */
    private AIAnalysisResult createDisabledResult(PlayerBehaviorData behaviorData) {
        return AIAnalysisResult.builder()
            .playerId(behaviorData.getPlayerId())
            .playerName(behaviorData.getPlayerName())
            .analysisTimestamp(System.currentTimeMillis())
            .responseTimeMs(0L)
            .isSuspicious(false)
            .confidenceScore(0.0)
            .cheatLikelihood(AIAnalysisResult.CheatLikelihood.VERY_LOW)
            .reasoningSummary("AI analysis is disabled")
            .recommendation("monitor")
            .build();
    }
    
    /**
     * Creates an error result when analysis fails
     */
    private AIAnalysisResult createErrorResult(PlayerBehaviorData behaviorData, String errorMessage) {
        return AIAnalysisResult.builder()
            .playerId(behaviorData.getPlayerId())
            .playerName(behaviorData.getPlayerName())
            .analysisTimestamp(System.currentTimeMillis())
            .responseTimeMs(0L)
            .isSuspicious(false)
            .confidenceScore(0.0)
            .cheatLikelihood(AIAnalysisResult.CheatLikelihood.VERY_LOW)
            .reasoningSummary("Analysis failed: " + errorMessage)
            .recommendation("monitor")
            .build();
    }
    
    /**
     * Checks if we should skip analysis due to recent analysis or rate limiting
     */
    private boolean shouldSkipAnalysis(String playerId) {
        Long lastAnalysis = lastAnalysisTime.get(playerId);
        if (lastAnalysis == null) return false;
        
        long timeSinceLastAnalysis = System.currentTimeMillis() - lastAnalysis;
        return timeSinceLastAnalysis < config.getAnalysisDelayMs();
    }
    
    /**
     * Logs analysis results if configured
     */
    private void logAnalysisResult(AIAnalysisResult result) {
        if (result.isSuspicious()) {
            CLARA.getInstance().getLogger().info(
                String.format("[AI-ANALYSIS] SUSPICIOUS: %s (Confidence: %.2f, Action: %s)",
                    result.getPlayerName(), result.getConfidenceScore(), result.getRecommendation())
            );
        } else if (config.isDetailedLogging()) {
            CLARA.getInstance().getLogger().info(
                String.format("[AI-ANALYSIS] CLEAR: %s (Confidence: %.2f)",
                    result.getPlayerName(), result.getConfidenceScore())
            );
        }
    }
    
    /**
     * Tests connection to Gemini AI
     */
    private void testConnection() {
        if (geminiClient.testConnection()) {
            CLARA.getInstance().getLogger().info("Gemini AI connection established successfully");
        } else {
            CLARA.getInstance().getLogger().warning("Failed to connect to Gemini AI - check your API key and configuration");
        }
    }
    
    /**
     * Shutdown the service and clean up resources
     */
    public void shutdown() {
        if (geminiClient != null) {
            geminiClient.shutdown();
        }
        analysisCache.clear();
        lastAnalysisTime.clear();
    }
}