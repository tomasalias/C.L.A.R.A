package AC.AI;

import AC.AI.Models.PlayerBehaviorData;
import AC.AI.Models.AIAnalysisResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Handles prompt engineering for Gemini AI analysis of player behavior.
 * Creates comprehensive prompts that help the AI understand Minecraft cheat patterns.
 */
public class PromptEngine {
    
    private final Gson gson;
    
    public PromptEngine() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }
    
    /**
     * Creates a comprehensive prompt for analyzing player behavior
     */
    public String createAnalysisPrompt(PlayerBehaviorData behaviorData) {
        StringBuilder prompt = new StringBuilder();
        
        // System context and role definition
        prompt.append("You are C.L.A.R.A (Cheat.Limiting.Adaptive.Response.Algorithm), an advanced AI anti-cheat system for Minecraft. ");
        prompt.append("Your role is to analyze player behavior data and determine if the player is likely using cheats or hacks.\n\n");
        
        // Instructions for analysis
        prompt.append("ANALYSIS INSTRUCTIONS:\n");
        prompt.append("1. Analyze the provided player behavior data for patterns consistent with cheating\n");
        prompt.append("2. Consider legitimate gameplay scenarios that might trigger false positives\n");
        prompt.append("3. Evaluate the severity and consistency of violations\n");
        prompt.append("4. Consider server performance and network conditions\n");
        prompt.append("5. Provide a confidence score from 0.0 (definitely legitimate) to 1.0 (definitely cheating)\n");
        prompt.append("6. Recommend appropriate action based on your analysis\n\n");
        
        // Minecraft-specific cheat patterns
        prompt.append("COMMON MINECRAFT CHEAT PATTERNS:\n");
        prompt.append("• Speed Hacks: Consistent movement speeds above normal limits (>6-7 m/s horizontal)\n");
        prompt.append("• Flight Hacks: Movement in air without falling, impossible vertical speeds\n");
        prompt.append("• Reach Hacks: Hitting entities/blocks beyond normal reach distance (>3.5 blocks)\n");
        prompt.append("• Timer Hacks: Packets arriving faster than normal (Minecraft runs at 20 TPS, packets every ~50ms)\n");
        prompt.append("• Bhop/Strafe: Artificial speed gain through movement exploitation\n");
        prompt.append("• NoFall: Taking no fall damage from impossible heights\n\n");
        
        // Legitimate scenarios to consider
        prompt.append("LEGITIMATE SCENARIOS TO CONSIDER:\n");
        prompt.append("• Server lag can cause packet clustering and false speed readings\n");
        prompt.append("• High ping can affect movement validation\n");
        prompt.append("• Players in vehicles (boats, horses) have different movement rules\n");
        prompt.append("• Environmental effects (ice, soul speed, jump boost potions)\n");
        prompt.append("• Recent teleportation or respawning can cause position irregularities\n");
        prompt.append("• Water/lava movement follows different physics\n\n");
        
        // Player data
        prompt.append("PLAYER BEHAVIOR DATA:\n");
        prompt.append("```json\n");
        prompt.append(gson.toJson(behaviorData));
        prompt.append("\n```\n\n");
        
        // Analysis framework
        prompt.append("ANALYSIS FRAMEWORK:\n");
        prompt.append("For each violation type, consider:\n");
        prompt.append("1. Is the violation consistent or sporadic?\n");
        prompt.append("2. Does the magnitude suggest intentional cheating?\n");
        prompt.append("3. Are there environmental factors that could explain it?\n");
        prompt.append("4. Is the player's ping/connection affecting measurements?\n");
        prompt.append("5. Are multiple violation types occurring simultaneously?\n\n");
        
        // Response format
        prompt.append("REQUIRED RESPONSE FORMAT:\n");
        prompt.append("Respond with a valid JSON object in exactly this format:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"isSuspicious\": boolean,\n");
        prompt.append("  \"confidenceScore\": number, // 0.0 to 1.0\n");
        prompt.append("  \"recommendation\": \"kick\" | \"ban\" | \"warn\" | \"monitor\" | \"clear\",\n");
        prompt.append("  \"reasoningSummary\": \"Brief explanation of your analysis\",\n");
        prompt.append("  \"suspiciousPatterns\": [\"pattern1\", \"pattern2\"],\n");
        prompt.append("  \"legitimateBehaviors\": [\"behavior1\", \"behavior2\"],\n");
        prompt.append("  \"violationAnalyses\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"violationType\": \"speed\" | \"flight\" | \"reach\" | \"timer\",\n");
        prompt.append("      \"isSuspicious\": boolean,\n");
        prompt.append("      \"suspicionLevel\": number, // 0.0 to 1.0\n");
        prompt.append("      \"analysis\": \"Explanation for this violation type\",\n");
        prompt.append("      \"pattern\": \"consistent\" | \"burst\" | \"gradual\" | \"random\",\n");
        prompt.append("      \"isLikelyFalsePositive\": boolean,\n");
        prompt.append("      \"falsePositiveReason\": \"Explanation if likely false positive\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"shouldTakeImmedateAction\": boolean,\n");
        prompt.append("  \"recommendedAction\": \"Action to take\",\n");
        prompt.append("  \"actionReason\": \"Why this action is recommended\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        // Final instructions
        prompt.append("IMPORTANT:\n");
        prompt.append("- Only respond with valid JSON, no additional text\n");
        prompt.append("- Be conservative with bans - prefer warnings for uncertain cases\n");
        prompt.append("- Consider that false positives harm legitimate players\n");
        prompt.append("- Account for server performance issues in your analysis\n");
        prompt.append("- Focus on pattern consistency rather than single violations\n");
        
        return prompt.toString();
    }
    
    /**
     * Creates a simplified prompt for quick analysis
     */
    public String createQuickAnalysisPrompt(PlayerBehaviorData behaviorData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze this Minecraft player for cheating. ");
        prompt.append("Respond with JSON: {\"isSuspicious\": boolean, \"confidence\": 0.0-1.0, \"reason\": \"brief explanation\"}\n\n");
        
        // Key data points only
        PlayerBehaviorData.MovementData movement = behaviorData.getMovementData();
        PlayerBehaviorData.ViolationData violations = behaviorData.getViolationData();
        PlayerBehaviorData.ContextData context = behaviorData.getContextData();
        
        prompt.append("Player: ").append(behaviorData.getPlayerName()).append("\n");
        prompt.append("Speed: ").append(movement.getTotalSpeed()).append(" m/s (normal: <6.5)\n");
        prompt.append("Flying: ").append(movement.isFlying() && !movement.isOnGround()).append("\n");
        prompt.append("Speed violations: ").append(violations.getSpeedViolationLevel()).append("\n");
        prompt.append("Flight violations: ").append(violations.getFlightViolationLevel()).append("\n");
        prompt.append("Reach violations: ").append(violations.getReachViolationLevel()).append("\n");
        prompt.append("Server TPS: ").append(context.getServerTPS()).append("\n");
        prompt.append("Player ping: ").append(context.getPlayerPing()).append("ms\n");
        
        return prompt.toString();
    }
    
    /**
     * Creates a prompt for batch analysis of multiple players
     */
    public String createBatchAnalysisPrompt(PlayerBehaviorData[] playersData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze these Minecraft players for cheating. Rank by suspicion level.\n\n");
        prompt.append("Respond with JSON array: [{\"player\": \"name\", \"suspicious\": boolean, \"confidence\": 0.0-1.0, \"priority\": \"high\"|\"medium\"|\"low\"}]\n\n");
        
        for (int i = 0; i < playersData.length; i++) {
            PlayerBehaviorData data = playersData[i];
            prompt.append("Player ").append(i + 1).append(":\n");
            prompt.append(gson.toJson(data)).append("\n\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Creates a specialized prompt for specific cheat types
     */
    public String createSpecializedPrompt(PlayerBehaviorData behaviorData, String focusType) {
        StringBuilder prompt = new StringBuilder();
        
        switch (focusType.toLowerCase()) {
            case "speed":
                prompt.append("Analyze this player specifically for SPEED HACKS:\n");
                prompt.append("Normal Minecraft movement speeds:\n");
                prompt.append("- Walking: 4.3 m/s\n");
                prompt.append("- Sprinting: 5.6 m/s\n");
                prompt.append("- Swimming: 2.2 m/s\n");
                prompt.append("- Flying (creative): 10.9 m/s\n");
                break;
                
            case "flight":
                prompt.append("Analyze this player specifically for FLIGHT HACKS:\n");
                prompt.append("Look for:\n");
                prompt.append("- Movement in air without falling\n");
                prompt.append("- Impossible vertical speeds\n");
                prompt.append("- Hovering at constant height\n");
                break;
                
            case "reach":
                prompt.append("Analyze this player specifically for REACH HACKS:\n");
                prompt.append("Normal reach distances:\n");
                prompt.append("- Block breaking: 5 blocks\n");
                prompt.append("- Entity interaction: 3 blocks\n");
                prompt.append("- Item pickup: 2 blocks\n");
                break;
                
            default:
                return createAnalysisPrompt(behaviorData);
        }
        
        prompt.append("\nPlayer Data:\n");
        prompt.append(gson.toJson(behaviorData));
        prompt.append("\n\nRespond with JSON: {\"suspicious\": boolean, \"confidence\": 0.0-1.0, \"analysis\": \"detailed explanation\"}");
        
        return prompt.toString();
    }
}