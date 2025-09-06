package AC.AI.Models;

import lombok.Data;
import lombok.Builder;

/**
 * Represents player behavior data for AI analysis.
 * Contains movement patterns, check violations, and contextual information.
 */
@Data
@Builder
public class PlayerBehaviorData {
    
    // Player identification
    private String playerName;
    private String playerId;
    private long timestamp;
    
    // Movement data
    private MovementData movementData;
    
    // Check violation data
    private ViolationData violationData;
    
    // Context data
    private ContextData contextData;
    
    @Data
    @Builder
    public static class MovementData {
        // Position data
        private double currentX, currentY, currentZ;
        private double previousX, previousY, previousZ;
        private double deltaX, deltaY, deltaZ;
        
        // Velocity and speed
        private double horizontalSpeed;
        private double verticalSpeed;
        private double totalSpeed;
        
        // Movement patterns
        private double averageSpeed;
        private double speedVariance;
        private boolean hasJumped;
        private boolean isOnGround;
        private boolean isInWater;
        private boolean isInLava;
        private boolean isFlying;
        
        // Rotation data
        private float yaw, pitch;
        private float deltaYaw, deltaPitch;
        private double rotationSpeed;
    }
    
    @Data
    @Builder
    public static class ViolationData {
        // Check types and their violation levels
        private int speedViolationLevel;
        private int flightViolationLevel;
        private int reachViolationLevel;
        private int timerViolationLevel;
        
        // Recent violation history (last 10 checks)
        private double[] recentSpeedViolations;
        private double[] recentFlightViolations;
        private double[] recentReachViolations;
        private double[] recentTimerViolations;
        
        // Violation patterns
        private boolean consistentViolations;
        private boolean suddenViolationIncrease;
        private String primaryViolationType;
        private double violationSeverity;
    }
    
    @Data
    @Builder  
    public static class ContextData {
        // Server context
        private double serverTPS;
        private int playerPing;
        private int playersOnline;
        private boolean isServerLagging;
        
        // Player context
        private boolean isPlayerOp;
        private boolean hasRecentlyRespawned;
        private boolean hasRecentlyTeleported;
        private boolean isInVehicle;
        private boolean isNearWater;
        private boolean isNearLava;
        private boolean isInCombat;
        
        // Environmental context
        private String currentBiome;
        private String currentWorld;
        private boolean isInNetherPortal;
        private boolean isInEndPortal;
        private boolean hasRecentlyChangedWorlds;
        
        // Time context
        private long timeOfDay;
        private long sessionDuration;
        private long timeSinceLastViolation;
    }
}