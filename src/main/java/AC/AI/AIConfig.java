package AC.AI;

import AC.CLARA;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Configuration manager for AI integration settings.
 * Handles loading and accessing configuration values for Gemini API and AI analysis.
 */
public class AIConfig {
    
    @Getter private static AIConfig instance;
    private FileConfiguration config;
    private File configFile;
    
    // AI Configuration
    @Getter private boolean aiEnabled;
    @Getter private String geminiApiKey;
    @Getter private String geminiApiUrl;
    @Getter private String modelName;
    @Getter private double temperature;
    @Getter private int maxTokens;
    @Getter private double confidenceThreshold;
    @Getter private boolean batchAnalysis;
    @Getter private int batchSize;
    @Getter private long analysisDelayMs;
    
    // Check Configuration
    @Getter private boolean speedCheckAiEnhanced;
    @Getter private boolean flightCheckAiEnhanced;
    @Getter private boolean reachCheckAiEnhanced;
    @Getter private boolean timerCheckAiEnhanced;
    
    // Action Configuration
    @Getter private String highConfidenceAction;
    @Getter private String mediumConfidenceAction;
    @Getter private double minActionConfidence;
    
    // Logging Configuration
    @Getter private boolean logAiAnalysis;
    @Getter private boolean detailedLogging;
    @Getter private String consoleLevel;
    
    public AIConfig() {
        instance = this;
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            // Create plugin data folder if it doesn't exist
            if (!CLARA.getInstance().getDataFolder().exists()) {
                CLARA.getInstance().getDataFolder().mkdirs();
            }
            
            configFile = new File(CLARA.getInstance().getDataFolder(), "config.yml");
            
            // Copy default config if it doesn't exist
            if (!configFile.exists()) {
                try (InputStream inputStream = CLARA.getInstance().getResource("config.yml")) {
                    if (inputStream != null) {
                        Files.copy(inputStream, configFile.toPath());
                    }
                }
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            loadValues();
            
        } catch (IOException e) {
            CLARA.getInstance().getLogger().log(Level.SEVERE, 
                "Failed to load AI configuration", e);
        }
    }
    
    private void loadValues() {
        // AI Settings
        aiEnabled = config.getBoolean("ai.enabled", true);
        geminiApiKey = config.getString("ai.gemini.api_key", "");
        geminiApiUrl = config.getString("ai.gemini.api_url", 
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent");
        modelName = config.getString("ai.gemini.model", "gemini-2.5-flash-lite");
        temperature = config.getDouble("ai.gemini.temperature", 0.3);
        maxTokens = config.getInt("ai.gemini.max_tokens", 1000);
        confidenceThreshold = config.getDouble("ai.gemini.confidence_threshold", 0.7);
        batchAnalysis = config.getBoolean("ai.gemini.batch_analysis", true);
        batchSize = config.getInt("ai.gemini.batch_size", 5);
        analysisDelayMs = config.getLong("ai.gemini.analysis_delay_ms", 2000);
        
        // Check Settings
        speedCheckAiEnhanced = config.getBoolean("checks.ai_enhanced.speed", true);
        flightCheckAiEnhanced = config.getBoolean("checks.ai_enhanced.flight", true);
        reachCheckAiEnhanced = config.getBoolean("checks.ai_enhanced.reach", true);
        timerCheckAiEnhanced = config.getBoolean("checks.ai_enhanced.timer", false);
        
        // Action Settings
        highConfidenceAction = config.getString("actions.high_confidence_action", "kick");
        mediumConfidenceAction = config.getString("actions.medium_confidence_action", "warn");
        minActionConfidence = config.getDouble("actions.min_action_confidence", 0.6);
        
        // Logging Settings
        logAiAnalysis = config.getBoolean("logging.log_ai_analysis", true);
        detailedLogging = config.getBoolean("logging.detailed_logging", false);
        consoleLevel = config.getString("logging.console_level", "INFO");
        
        // Validate API key
        if (aiEnabled && (geminiApiKey == null || geminiApiKey.trim().isEmpty() || 
            geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE"))) {
            CLARA.getInstance().getLogger().warning(
                "AI is enabled but no valid Gemini API key is configured. " +
                "Please set your API key in config.yml");
            aiEnabled = false;
        }
    }
    
    public void reloadConfig() {
        loadConfig();
        CLARA.getInstance().getLogger().info("AI configuration reloaded");
    }
    
    public boolean isValidConfiguration() {
        return aiEnabled && geminiApiKey != null && !geminiApiKey.trim().isEmpty() &&
               !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }
}