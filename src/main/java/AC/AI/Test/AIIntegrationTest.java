package AC.AI.Test;

import AC.AI.Models.PlayerBehaviorData;
import AC.AI.PromptEngine;

/**
 * Simple test to validate AI integration components
 */
public class AIIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("Testing C.L.A.R.A AI Integration...");
        
        // Test 1: Behavior Data Model Creation
        try {
            PlayerBehaviorData behaviorData = PlayerBehaviorData.builder()
                .playerName("TestPlayer")
                .playerId("test-uuid")
                .timestamp(System.currentTimeMillis())
                .movementData(PlayerBehaviorData.MovementData.builder()
                    .currentX(100.0)
                    .currentY(64.0)
                    .currentZ(100.0)
                    .horizontalSpeed(5.6)
                    .totalSpeed(5.6)
                    .isOnGround(true)
                    .build())
                .violationData(PlayerBehaviorData.ViolationData.builder()
                    .speedViolationLevel(3)
                    .primaryViolationType("speed")
                    .violationSeverity(0.6)
                    .build())
                .contextData(PlayerBehaviorData.ContextData.builder()
                    .serverTPS(20.0)
                    .playerPing(45)
                    .playersOnline(10)
                    .isServerLagging(false)
                    .build())
                .build();
            
            System.out.println("✓ Behavior Data Model: OK");
            
        } catch (Exception e) {
            System.out.println("✗ Behavior Data Model: FAILED - " + e.getMessage());
        }
        
        // Test 2: Prompt Engine
        try {
            PromptEngine promptEngine = new PromptEngine();
            
            PlayerBehaviorData testData = PlayerBehaviorData.builder()
                .playerName("SpeedHacker")
                .playerId("test-uuid")
                .timestamp(System.currentTimeMillis())
                .movementData(PlayerBehaviorData.MovementData.builder()
                    .horizontalSpeed(15.0) // Suspicious speed
                    .totalSpeed(15.0)
                    .isOnGround(true)
                    .isFlying(false)
                    .build())
                .violationData(PlayerBehaviorData.ViolationData.builder()
                    .speedViolationLevel(8)
                    .primaryViolationType("speed")
                    .violationSeverity(0.9)
                    .build())
                .contextData(PlayerBehaviorData.ContextData.builder()
                    .serverTPS(19.8)
                    .playerPing(30)
                    .isServerLagging(false)
                    .build())
                .build();
            
            String prompt = promptEngine.createAnalysisPrompt(testData);
            
            if (prompt != null && prompt.length() > 1000) {
                System.out.println("✓ Prompt Engine: OK (" + prompt.length() + " characters)");
            } else {
                System.out.println("✗ Prompt Engine: FAILED - Prompt too short or null");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Prompt Engine: FAILED - " + e.getMessage());
        }
        
        // Test 3: Quick Analysis Prompt
        try {
            PromptEngine promptEngine = new PromptEngine();
            PlayerBehaviorData testData = PlayerBehaviorData.builder()
                .playerName("TestPlayer")
                .playerId("test-uuid")
                .timestamp(System.currentTimeMillis())
                .movementData(PlayerBehaviorData.MovementData.builder()
                    .horizontalSpeed(6.0)
                    .totalSpeed(6.0)
                    .build())
                .violationData(PlayerBehaviorData.ViolationData.builder()
                    .speedViolationLevel(2)
                    .build())
                .contextData(PlayerBehaviorData.ContextData.builder()
                    .serverTPS(20.0)
                    .playerPing(50)
                    .build())
                .build();
            
            String quickPrompt = promptEngine.createQuickAnalysisPrompt(testData);
            
            if (quickPrompt != null && quickPrompt.contains("TestPlayer")) {
                System.out.println("✓ Quick Analysis Prompt: OK");
            } else {
                System.out.println("✗ Quick Analysis Prompt: FAILED");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Quick Analysis Prompt: FAILED - " + e.getMessage());
        }
        
        System.out.println("\nAI Integration Test Complete!");
        System.out.println("Note: This test validates data models and prompts only.");
        System.out.println("Full API testing requires a valid Gemini API key and network connection.");
    }
}