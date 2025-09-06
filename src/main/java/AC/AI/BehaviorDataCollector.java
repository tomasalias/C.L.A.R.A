package AC.AI;

import AC.AI.Models.PlayerBehaviorData;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.CheckUtils.Position;
import AC.CLARA;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and aggregates player behavior data for AI analysis.
 * Integrates with existing PlayerData system and check results.
 */
public class BehaviorDataCollector {
    
    private static BehaviorDataCollector instance;
    
    // Store violation histories for players
    private final ConcurrentHashMap<UUID, ViolationHistory> violationHistories;
    
    public BehaviorDataCollector() {
        instance = this;
        this.violationHistories = new ConcurrentHashMap<>();
    }
    
    public static BehaviorDataCollector getInstance() {
        return instance;
    }
    
    /**
     * Collects comprehensive behavior data for a player
     */
    public PlayerBehaviorData collectBehaviorData(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return null;
        }
        
        PlayerData playerData = CLARA.getPlayerData(playerId);
        if (playerData == null) {
            return null;
        }
        
        ViolationHistory violations = violationHistories.computeIfAbsent(playerId, k -> new ViolationHistory());
        
        return PlayerBehaviorData.builder()
            .playerName(player.getName())
            .playerId(playerId.toString())
            .timestamp(System.currentTimeMillis())
            .movementData(collectMovementData(player, playerData))
            .violationData(collectViolationData(violations))
            .contextData(collectContextData(player, playerData))
            .build();
    }
    
    /**
     * Collects movement-related data
     */
    private PlayerBehaviorData.MovementData collectMovementData(Player player, PlayerData playerData) {
        // Get current and previous positions
        Position currentPos = playerData.getCurrentPosition();
        Position previousPos = playerData.getPreviousPosition();
        
        double deltaX = 0, deltaY = 0, deltaZ = 0;
        double horizontalSpeed = 0, verticalSpeed = 0, totalSpeed = 0;
        
        if (currentPos != null && previousPos != null) {
            deltaX = currentPos.getX() - previousPos.getX();
            deltaY = currentPos.getY() - previousPos.getY();
            deltaZ = currentPos.getZ() - previousPos.getZ();
            
            horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20; // Convert to m/s
            verticalSpeed = Math.abs(deltaY) * 20;
            totalSpeed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 20;
        }
        
        return PlayerBehaviorData.MovementData.builder()
            .currentX(currentPos != null ? currentPos.getX() : 0)
            .currentY(currentPos != null ? currentPos.getY() : 0)
            .currentZ(currentPos != null ? currentPos.getZ() : 0)
            .previousX(previousPos != null ? previousPos.getX() : 0)
            .previousY(previousPos != null ? previousPos.getY() : 0)
            .previousZ(previousPos != null ? previousPos.getZ() : 0)
            .deltaX(deltaX)
            .deltaY(deltaY)
            .deltaZ(deltaZ)
            .horizontalSpeed(horizontalSpeed)
            .verticalSpeed(verticalSpeed)
            .totalSpeed(totalSpeed)
            .averageSpeed(playerData.getAverageSpeed())
            .speedVariance(calculateSpeedVariance(playerData))
            .hasJumped(deltaY > 0.42) // Standard jump height
            .isOnGround(player.isOnGround())
            .isInWater(player.isInWater())
            .isFlying(player.isFlying())
            .yaw(player.getLocation().getYaw())
            .pitch(player.getLocation().getPitch())
            .deltaYaw(calculateYawDelta(playerData))
            .deltaPitch(calculatePitchDelta(playerData))
            .rotationSpeed(calculateRotationSpeed(playerData))
            .build();
    }
    
    /**
     * Collects violation data from check systems
     */
    private PlayerBehaviorData.ViolationData collectViolationData(ViolationHistory violations) {
        return PlayerBehaviorData.ViolationData.builder()
            .speedViolationLevel(violations.getSpeedViolations())
            .flightViolationLevel(violations.getFlightViolations())
            .reachViolationLevel(violations.getReachViolations())
            .timerViolationLevel(violations.getTimerViolations())
            .recentSpeedViolations(violations.getRecentSpeedViolations())
            .recentFlightViolations(violations.getRecentFlightViolations())
            .recentReachViolations(violations.getRecentReachViolations())
            .recentTimerViolations(violations.getRecentTimerViolations())
            .consistentViolations(violations.hasConsistentViolations())
            .suddenViolationIncrease(violations.hasSuddenIncrease())
            .primaryViolationType(violations.getPrimaryViolationType())
            .violationSeverity(violations.getOverallSeverity())
            .build();
    }
    
    /**
     * Collects contextual data
     */
    private PlayerBehaviorData.ContextData collectContextData(Player player, PlayerData playerData) {
        return PlayerBehaviorData.ContextData.builder()
            .serverTPS(getServerTPS())
            .playerPing(playerData.getPing())
            .playersOnline(Bukkit.getOnlinePlayers().size())
            .isServerLagging(getServerTPS() < 18.0)
            .isPlayerOp(player.isOp())
            .hasRecentlyRespawned(hasRecentlyRespawned(player.getUniqueId()))
            .hasRecentlyTeleported(playerData.hasRecentlyTeleported())
            .isInVehicle(player.isInsideVehicle())
            .isNearWater(isNearWater(player))
            .isInCombat(playerData.isInCombat())
            .currentBiome(player.getLocation().getBlock().getBiome().toString())
            .currentWorld(player.getWorld().getName())
            .timeOfDay(player.getWorld().getTime())
            .sessionDuration(System.currentTimeMillis() - playerData.getJoinTime())
            .timeSinceLastViolation(playerData.getTimeSinceLastViolation())
            .build();
    }
    
    /**
     * Records a violation for tracking patterns
     */
    public void recordViolation(UUID playerId, String violationType, double severity) {
        ViolationHistory history = violationHistories.computeIfAbsent(playerId, k -> new ViolationHistory());
        history.recordViolation(violationType, severity);
    }
    
    /**
     * Utility methods for calculations
     */
    private double calculateSpeedVariance(PlayerData playerData) {
        // Simplified variance calculation
        // In a real implementation, this would calculate variance from stored speed values
        return 0.0; // Placeholder
    }
    
    private float calculateYawDelta(PlayerData playerData) {
        // Calculate yaw rotation change
        return 0.0f; // Placeholder
    }
    
    private float calculatePitchDelta(PlayerData playerData) {
        // Calculate pitch rotation change
        return 0.0f; // Placeholder
    }
    
    private double calculateRotationSpeed(PlayerData playerData) {
        // Calculate overall rotation speed
        return 0.0; // Placeholder
    }
    
    private double getServerTPS() {
        try {
            return Bukkit.getServer().getTPS()[0]; // Get 1-minute TPS
        } catch (Exception e) {
            return 20.0; // Default if TPS not available
        }
    }
    
    private boolean hasRecentlyRespawned(UUID playerId) {
        Long respawnTime = CLARA.getInstance().getPlayerRespawnMap().get(playerId);
        if (respawnTime == null) return false;
        
        return System.currentTimeMillis() - respawnTime < 5000; // 5 seconds
    }
    
    private boolean isNearWater(Player player) {
        // Check blocks around player for water
        return player.getLocation().getBlock().getType().toString().contains("WATER") ||
               player.getLocation().add(0, -1, 0).getBlock().getType().toString().contains("WATER");
    }
    
    /**
     * Inner class to track violation history for a player
     */
    private static class ViolationHistory {
        private int speedViolations = 0;
        private int flightViolations = 0;
        private int reachViolations = 0;
        private int timerViolations = 0;
        
        private final double[] recentSpeedViolations = new double[10];
        private final double[] recentFlightViolations = new double[10];
        private final double[] recentReachViolations = new double[10];
        private final double[] recentTimerViolations = new double[10];
        
        private int recentIndex = 0;
        private long lastViolationTime = 0;
        private String lastViolationType = "";
        
        public void recordViolation(String type, double severity) {
            lastViolationTime = System.currentTimeMillis();
            lastViolationType = type;
            
            switch (type.toLowerCase()) {
                case "speed":
                    speedViolations++;
                    recentSpeedViolations[recentIndex % 10] = severity;
                    break;
                case "flight":
                    flightViolations++;
                    recentFlightViolations[recentIndex % 10] = severity;
                    break;
                case "reach":
                    reachViolations++;
                    recentReachViolations[recentIndex % 10] = severity;
                    break;
                case "timer":
                    timerViolations++;
                    recentTimerViolations[recentIndex % 10] = severity;
                    break;
            }
            recentIndex++;
        }
        
        // Getters
        public int getSpeedViolations() { return speedViolations; }
        public int getFlightViolations() { return flightViolations; }
        public int getReachViolations() { return reachViolations; }
        public int getTimerViolations() { return timerViolations; }
        public double[] getRecentSpeedViolations() { return recentSpeedViolations; }
        public double[] getRecentFlightViolations() { return recentFlightViolations; }
        public double[] getRecentReachViolations() { return recentReachViolations; }
        public double[] getRecentTimerViolations() { return recentTimerViolations; }
        
        public boolean hasConsistentViolations() {
            int totalViolations = speedViolations + flightViolations + reachViolations + timerViolations;
            return totalViolations >= 5;
        }
        
        public boolean hasSuddenIncrease() {
            // Check if violations have increased rapidly in recent time
            long timeSinceFirst = System.currentTimeMillis() - lastViolationTime;
            int totalViolations = speedViolations + flightViolations + reachViolations + timerViolations;
            return totalViolations >= 3 && timeSinceFirst < 10000; // 3 violations in 10 seconds
        }
        
        public String getPrimaryViolationType() {
            int maxViolations = Math.max(Math.max(speedViolations, flightViolations), 
                                       Math.max(reachViolations, timerViolations));
            
            if (maxViolations == 0) return "none";
            if (speedViolations == maxViolations) return "speed";
            if (flightViolations == maxViolations) return "flight";
            if (reachViolations == maxViolations) return "reach";
            return "timer";
        }
        
        public double getOverallSeverity() {
            int totalViolations = speedViolations + flightViolations + reachViolations + timerViolations;
            if (totalViolations == 0) return 0.0;
            
            // Simple severity calculation based on violation count and recency
            double severityScore = Math.min(totalViolations / 10.0, 1.0);
            
            // Increase severity if violations are recent
            if (System.currentTimeMillis() - lastViolationTime < 5000) {
                severityScore *= 1.5;
            }
            
            return Math.min(severityScore, 1.0);
        }
    }
}