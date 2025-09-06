package AC.Commands;

import AC.AI.AIAnalysisService;
import AC.AI.BehaviorDataCollector;
import AC.AI.Integration.AIEnhancedCheckHandler;
import AC.AI.Models.AIAnalysisResult;
import AC.AI.Models.PlayerBehaviorData;
import AC.CLARA;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command for managing and testing AI functionality in C.L.A.R.A
 */
public class AICommand implements CommandExecutor, TabCompleter {
    
    private final AIAnalysisService aiService;
    private final BehaviorDataCollector dataCollector;
    private final AIEnhancedCheckHandler checkHandler;
    
    public AICommand() {
        this.aiService = CLARA.getInstance().getAiAnalysisService();
        this.dataCollector = CLARA.getInstance().getBehaviorDataCollector();
        this.checkHandler = CLARA.getInstance().getAiEnhancedCheckHandler();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("clara.ai") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use AI commands.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
                showStatus(sender);
                break;
                
            case "test":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ai test <player>");
                    return true;
                }
                testPlayer(sender, args[1]);
                break;
                
            case "analyze":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ai analyze <player>");
                    return true;
                }
                analyzePlayer(sender, args[1]);
                break;
                
            case "reload":
                reloadConfig(sender);
                break;
                
            case "cleanup":
                cleanup(sender);
                break;
                
            case "testconnection":
                testConnection(sender);
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== C.L.A.R.A AI Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ai status" + ChatColor.WHITE + " - Show AI system status");
        sender.sendMessage(ChatColor.YELLOW + "/ai test <player>" + ChatColor.WHITE + " - Test AI analysis on player");
        sender.sendMessage(ChatColor.YELLOW + "/ai analyze <player>" + ChatColor.WHITE + " - Perform full AI analysis");
        sender.sendMessage(ChatColor.YELLOW + "/ai testconnection" + ChatColor.WHITE + " - Test Gemini API connection");
        sender.sendMessage(ChatColor.YELLOW + "/ai reload" + ChatColor.WHITE + " - Reload AI configuration");
        sender.sendMessage(ChatColor.YELLOW + "/ai cleanup" + ChatColor.WHITE + " - Clean up AI caches");
    }
    
    private void showStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== C.L.A.R.A AI Status ===");
        
        // AI Service Status
        if (aiService != null && aiService.getConfig().isValidConfiguration()) {
            sender.sendMessage(ChatColor.GREEN + "✓ AI Service: ENABLED");
            sender.sendMessage(ChatColor.WHITE + "  Model: " + aiService.getConfig().getModelName());
            sender.sendMessage(ChatColor.WHITE + "  Confidence Threshold: " + aiService.getConfig().getConfidenceThreshold());
            sender.sendMessage(ChatColor.WHITE + "  Batch Analysis: " + (aiService.getConfig().isBatchAnalysis() ? "ON" : "OFF"));
        } else {
            sender.sendMessage(ChatColor.RED + "✗ AI Service: DISABLED");
            sender.sendMessage(ChatColor.YELLOW + "  Check your API key configuration");
        }
        
        // Enhanced Checks Status
        sender.sendMessage(ChatColor.GOLD + "Enhanced Checks:");
        if (aiService != null && aiService.getConfig().isValidConfiguration()) {
            sender.sendMessage("  " + (aiService.getConfig().isSpeedCheckAiEnhanced() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") + ChatColor.WHITE + " Speed Check");
            sender.sendMessage("  " + (aiService.getConfig().isFlightCheckAiEnhanced() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") + ChatColor.WHITE + " Flight Check");
            sender.sendMessage("  " + (aiService.getConfig().isReachCheckAiEnhanced() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") + ChatColor.WHITE + " Reach Check");
            sender.sendMessage("  " + (aiService.getConfig().isTimerCheckAiEnhanced() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗") + ChatColor.WHITE + " Timer Check");
        } else {
            sender.sendMessage(ChatColor.RED + "  All checks: DISABLED (AI not configured)");
        }
        
        // Online Players
        sender.sendMessage(ChatColor.GOLD + "Online Players: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
    }
    
    private void testPlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }
        
        if (aiService == null || !aiService.getConfig().isValidConfiguration()) {
            sender.sendMessage(ChatColor.RED + "AI service is not available or configured.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Testing AI analysis on " + target.getName() + "...");
        
        // Simulate a check violation for testing
        checkHandler.processCheckViolation(target.getUniqueId(), "speed", 3.0, "Test violation");
        
        sender.sendMessage(ChatColor.GREEN + "Test initiated. Check console for results.");
    }
    
    private void analyzePlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }
        
        if (aiService == null || !aiService.getConfig().isValidConfiguration()) {
            sender.sendMessage(ChatColor.RED + "AI service is not available or configured.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Analyzing " + target.getName() + "...");
        
        // Collect behavior data
        PlayerBehaviorData behaviorData = dataCollector.collectBehaviorData(target.getUniqueId());
        if (behaviorData == null) {
            sender.sendMessage(ChatColor.RED + "Could not collect behavior data for " + target.getName());
            return;
        }
        
        // Perform AI analysis
        CompletableFuture<AIAnalysisResult> analysisResult = aiService.analyzePlayerBehavior(behaviorData);
        
        analysisResult.thenAccept(result -> {
            sender.sendMessage(ChatColor.GOLD + "=== AI Analysis Result ===");
            sender.sendMessage(ChatColor.WHITE + "Player: " + target.getName());
            sender.sendMessage(ChatColor.WHITE + "Suspicious: " + (result.isSuspicious() ? ChatColor.RED + "YES" : ChatColor.GREEN + "NO"));
            sender.sendMessage(ChatColor.WHITE + "Confidence: " + String.format("%.1f%%", result.getConfidenceScore() * 100));
            sender.sendMessage(ChatColor.WHITE + "Likelihood: " + result.getCheatLikelihood().getDisplayName());
            sender.sendMessage(ChatColor.WHITE + "Recommendation: " + ChatColor.YELLOW + result.getRecommendation());
            
            if (result.getReasoningSummary() != null) {
                sender.sendMessage(ChatColor.WHITE + "Reason: " + ChatColor.GRAY + result.getReasoningSummary());
            }
            
            sender.sendMessage(ChatColor.WHITE + "Response Time: " + result.getResponseTimeMs() + "ms");
            
        }).exceptionally(throwable -> {
            sender.sendMessage(ChatColor.RED + "Analysis failed: " + throwable.getMessage());
            return null;
        });
    }
    
    private void testConnection(CommandSender sender) {
        if (aiService == null || !aiService.getConfig().isValidConfiguration()) {
            sender.sendMessage(ChatColor.RED + "AI service is not configured.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Testing Gemini API connection...");
        
        // Test connection in async to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                boolean connected = aiService.getGeminiClient().testConnection();
                if (connected) {
                    sender.sendMessage(ChatColor.GREEN + "✓ Gemini API connection successful!");
                } else {
                    sender.sendMessage(ChatColor.RED + "✗ Gemini API connection failed.");
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "✗ Connection test failed: " + e.getMessage());
            }
        });
    }
    
    private void reloadConfig(CommandSender sender) {
        try {
            if (aiService != null && aiService.getConfig() != null) {
                aiService.getConfig().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "AI configuration reloaded successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "AI service is not available.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload configuration: " + e.getMessage());
        }
    }
    
    private void cleanup(CommandSender sender) {
        try {
            if (checkHandler != null) {
                checkHandler.cleanupCache();
            }
            sender.sendMessage(ChatColor.GREEN + "AI caches cleaned up successfully.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to cleanup: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("clara.ai") && !sender.isOp()) {
            return null;
        }
        
        if (args.length == 1) {
            return Arrays.asList("status", "test", "analyze", "reload", "cleanup", "testconnection")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("test") || args[0].equalsIgnoreCase("analyze"))) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return null;
    }
}