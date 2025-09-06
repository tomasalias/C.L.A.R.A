package AC.AI.Integration;

import AC.AI.AIAnalysisService;
import AC.AI.BehaviorDataCollector;
import AC.AI.Models.AIAnalysisResult;
import AC.AI.Models.PlayerBehaviorData;
import AC.CLARA;
import AC.Utils.PluginUtils.KickMessages;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Enhanced check handler that integrates AI analysis with traditional anti-cheat checks.
 * This class provides a bridge between existing check systems and AI-powered analysis.
 */
public class AIEnhancedCheckHandler {
    
    private static AIEnhancedCheckHandler instance;
    
    private final AIAnalysisService aiService;
    private final BehaviorDataCollector dataCollector;
    
    // Track players currently being analyzed to avoid spam
    private final ConcurrentHashMap<UUID, Long> analysisInProgress;
    
    // Cache of recent AI decisions to avoid re-analysis
    private final ConcurrentHashMap<UUID, AIDecisionCache> decisionCache;
    
    public AIEnhancedCheckHandler() {
        instance = this;
        this.aiService = CLARA.getInstance().getAiAnalysisService();
        this.dataCollector = CLARA.getInstance().getBehaviorDataCollector();
        this.analysisInProgress = new ConcurrentHashMap<>();
        this.decisionCache = new ConcurrentHashMap<>();
    }
    
    public static AIEnhancedCheckHandler getInstance() {
        return instance;
    }
    
    /**
     * Process a traditional check result and potentially enhance it with AI analysis
     */
    public void processCheckViolation(UUID playerId, String checkType, double severity, String details) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        
        // Record the violation for behavior tracking
        dataCollector.recordViolation(playerId, checkType, severity);
        
        // Check if this check type is AI-enhanced
        if (!isAIEnhancedCheck(checkType)) {
            handleTraditionalViolation(player, checkType, severity, details);
            return;
        }
        
        // Skip if we're already analyzing this player
        if (isAnalysisInProgress(playerId)) {
            return;
        }
        
        // Check cached decision first
        AIDecisionCache cached = decisionCache.get(playerId);
        if (cached != null && !cached.isExpired()) {
            if (cached.decision.isSuspicious() && cached.decision.shouldTakeAction(0.6)) {
                executeAIRecommendedAction(player, cached.decision);
            }
            return;
        }
        
        // Mark analysis as in progress
        analysisInProgress.put(playerId, System.currentTimeMillis());
        
        // Collect behavior data and analyze with AI
        PlayerBehaviorData behaviorData = dataCollector.collectBehaviorData(playerId);
        if (behaviorData == null) {
            analysisInProgress.remove(playerId);
            handleTraditionalViolation(player, checkType, severity, details);
            return;
        }
        
        // Perform AI analysis asynchronously
        aiService.analyzePlayerBehavior(behaviorData).thenAccept(result -> {
            analysisInProgress.remove(playerId);
            
            // Cache the result
            decisionCache.put(playerId, new AIDecisionCache(result));
            
            // Handle the AI decision
            handleAIAnalysisResult(player, result, checkType, severity);
            
        }).exceptionally(throwable -> {
            analysisInProgress.remove(playerId);
            CLARA.getInstance().getLogger().log(Level.WARNING, 
                "AI analysis failed for " + player.getName(), throwable);
            
            // Fall back to traditional handling
            handleTraditionalViolation(player, checkType, severity, details);
            return null;
        });
    }
    
    /**
     * Handle AI analysis result and take appropriate action
     */
    private void handleAIAnalysisResult(Player player, AIAnalysisResult result, String checkType, double severity) {
        // Log the analysis if configured
        if (aiService.getConfig().isLogAiAnalysis()) {
            CLARA.getInstance().getLogger().info(
                String.format("[AI-ENHANCED] %s triggered %s check (severity: %.2f) - AI: %s (confidence: %.2f)",
                    player.getName(), checkType, severity, 
                    result.isSuspicious() ? "SUSPICIOUS" : "CLEAR", result.getConfidenceScore())
            );
        }
        
        // Decide action based on AI analysis
        if (result.isSuspicious() && result.shouldTakeAction(aiService.getConfig().getMinActionConfidence())) {
            executeAIRecommendedAction(player, result);
        } else if (!result.isSuspicious() && severity > getHighSeverityThreshold(checkType)) {
            // AI says it's not suspicious, but traditional check shows high severity
            // This might be a false positive - just warn instead of kick/ban
            warnPlayerForPotentialFalsePositive(player, checkType, result);
        } else {
            // Either low severity or AI is uncertain - just monitor
            if (aiService.getConfig().isDetailedLogging()) {
                CLARA.getInstance().getLogger().info(
                    String.format("[AI-ENHANCED] %s - No action taken (AI confidence: %.2f, severity: %.2f)",
                        player.getName(), result.getConfidenceScore(), severity)
                );
            }
        }
    }
    
    /**
     * Execute the action recommended by AI
     */
    private void executeAIRecommendedAction(Player player, AIAnalysisResult result) {
        String action = result.getRecommendation().toLowerCase();
        String reason = result.getActionReason() != null ? result.getActionReason() : 
                       "AI detected suspicious behavior pattern";
        
        switch (action) {
            case "kick":
                kickPlayerWithAIReason(player, result);
                break;
                
            case "ban":
                banPlayerWithAIReason(player, result);
                break;
                
            case "warn":
                warnPlayerWithAIReason(player, result);
                break;
                
            default:
                // Monitor or unclear - just log
                CLARA.getInstance().getLogger().info(
                    String.format("[AI-ENHANCED] %s flagged for monitoring - %s", 
                        player.getName(), reason)
                );
        }
    }
    
    /**
     * Kick player with AI-generated reasoning
     */
    private void kickPlayerWithAIReason(Player player, AIAnalysisResult result) {
        String message = ChatColor.RED + "C.L.A.R.A AI Detection\n" +
                        ChatColor.YELLOW + "Suspicious behavior detected\n" +
                        ChatColor.WHITE + "Confidence: " + String.format("%.0f%%", result.getConfidenceScore() * 100) + "\n" +
                        ChatColor.GRAY + "Reason: " + (result.getReasoningSummary() != null ? 
                            result.getReasoningSummary() : "Multiple violations detected");
        
        Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
            player.kickPlayer(message);
        });
        
        // Broadcast to staff
        broadcastToStaff(ChatColor.RED + "[C.L.A.R.A AI] " + ChatColor.WHITE + player.getName() + 
                         " kicked for suspicious behavior (confidence: " + 
                         String.format("%.0f%%", result.getConfidenceScore() * 100) + ")");
    }
    
    /**
     * Ban player with AI-generated reasoning (for very high confidence cases)
     */
    private void banPlayerWithAIReason(Player player, AIAnalysisResult result) {
        // For now, just kick - banning would require additional configuration
        kickPlayerWithAIReason(player, result);
        
        CLARA.getInstance().getLogger().warning(
            String.format("[AI-ENHANCED] %s recommended for BAN by AI (confidence: %.2f)", 
                player.getName(), result.getConfidenceScore())
        );
    }
    
    /**
     * Warn player about detected behavior
     */
    private void warnPlayerWithAIReason(Player player, AIAnalysisResult result) {
        String message = ChatColor.YELLOW + "[C.L.A.R.A] " + ChatColor.WHITE + 
                        "Suspicious behavior detected. Please play fairly.";
        player.sendMessage(message);
        
        broadcastToStaff(ChatColor.YELLOW + "[C.L.A.R.A AI] " + ChatColor.WHITE + player.getName() + 
                         " warned for suspicious behavior (confidence: " + 
                         String.format("%.0f%%", result.getConfidenceScore() * 100) + ")");
    }
    
    /**
     * Warn about potential false positive
     */
    private void warnPlayerForPotentialFalsePositive(Player player, String checkType, AIAnalysisResult result) {
        CLARA.getInstance().getLogger().info(
            String.format("[AI-ENHANCED] %s triggered %s but AI suggests false positive (confidence: %.2f)", 
                player.getName(), checkType, result.getConfidenceScore())
        );
    }
    
    /**
     * Handle traditional violation without AI enhancement
     */
    private void handleTraditionalViolation(Player player, String checkType, double severity, String details) {
        // Use existing kick messages system
        if (severity >= getKickThreshold(checkType)) {
            KickMessages.kickPlayerForInvalidPacket(player, getKickCode(checkType));
        }
    }
    
    /**
     * Check if a specific check type is enhanced by AI
     */
    private boolean isAIEnhancedCheck(String checkType) {
        if (aiService == null || !aiService.getConfig().isValidConfiguration()) {
            return false;
        }
        
        switch (checkType.toLowerCase()) {
            case "speed":
                return aiService.getConfig().isSpeedCheckAiEnhanced();
            case "flight":
                return aiService.getConfig().isFlightCheckAiEnhanced();
            case "reach":
                return aiService.getConfig().isReachCheckAiEnhanced();
            case "timer":
                return aiService.getConfig().isTimerCheckAiEnhanced();
            default:
                return false;
        }
    }
    
    /**
     * Check if analysis is currently in progress for a player
     */
    private boolean isAnalysisInProgress(UUID playerId) {
        Long analysisStart = analysisInProgress.get(playerId);
        if (analysisStart == null) return false;
        
        // Remove stale analysis markers (older than 30 seconds)
        if (System.currentTimeMillis() - analysisStart > 30000) {
            analysisInProgress.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Broadcast message to staff members
     */
    private void broadcastToStaff(String message) {
        Bukkit.getOnlinePlayers().stream()
            .filter(Player::isOp)
            .forEach(staff -> staff.sendMessage(message));
    }
    
    // Utility methods for thresholds and codes
    private double getHighSeverityThreshold(String checkType) {
        return 5.0; // Configurable threshold
    }
    
    private double getKickThreshold(String checkType) {
        return 10.0; // Configurable threshold
    }
    
    private String getKickCode(String checkType) {
        switch (checkType.toLowerCase()) {
            case "speed": return "S";
            case "flight": return "F"; 
            case "reach": return "R";
            case "timer": return "T";
            default: return "X";
        }
    }
    
    /**
     * Cache class for AI decisions
     */
    private static class AIDecisionCache {
        final AIAnalysisResult decision;
        final long timestamp;
        
        AIDecisionCache(AIAnalysisResult decision) {
            this.decision = decision;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30 second cache
        }
    }
    
    /**
     * Clean up expired cache entries
     */
    public void cleanupCache() {
        decisionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}