package AC.Checks.Movement;

import AC.Utils.CheckUtils.FastMath; // Utility class for optimized vector and speed calculations
import AC.Utils.PluginUtils.KickMessages; // Handles player kick logic and messaging
import AC.Utils.PluginUtils.PlayerOpStorage; // Tracks operator (OP) status for players
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D; // 3D vector for positional data
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * SpeedCheckA monitors a player's movement between packets and flags/kicks them if horizontal movement exceeds expected limits.
 * Async-safe and designed to minimize performance impact during real-time gameplay.
 */
public class SpeedCheckA {
    // Base movement threshold for horizontal-only motion
    private static final double DEFAULT_THRESHOLD = 0.5;

    // Adjusted threshold if vertical movement is detected (players falling/jumping may move faster)
    private static final double VERTICAL_ADJUSTED_THRESHOLD = 0.8;

    @Getter
    private final UUID playerUUID; // Player identifier (used for logs / traceability)

    // Previous and current positions — null until populated
    private Vector3D positionA = null;
    private Vector3D positionB = null;

    private boolean processing = false; // Flag to prevent concurrent analysis of overlapping packets

    private final ExecutorService executorService; // Worker thread pool for async movement processing
    private Boolean isOpCached = null; // Lazily cached OP status for the player
    private final PlayerOpStorage playerOpStorage;

    /**
     * Constructor wiring core dependencies.
     */
    public SpeedCheckA(UUID playerUUID, PlayerOpStorage playerOpStorage, ExecutorService executorService) {
        this.playerUUID = playerUUID;
        this.playerOpStorage = playerOpStorage;
        this.executorService = executorService;
    }

    /**
     * Main entry point called for each incoming movement update.
     * Processes and validates movement asynchronously.
     */
    public void handlePosition(Player player, double x, double y, double z) {
        // Only resolve and cache OP status once to avoid repeated lookups
        if (isOpCached == null) {
            isOpCached = playerOpStorage.isPlayerOperator(player);
        }

        // Skip checking if player is OP or a movement check is already underway
        if (isOpCached || processing) {
            return;
        }

        // Mark as processing to avoid overlapping packet checks
        processing = true;

        // Submit async movement analysis to prevent main thread lag
        executorService.submit(() -> {
            // If this is the first packet, store positionA and return
            if (positionA == null) {
                positionA = new Vector3D(x, y, z);
                processing = false;
                return;
            }

            // Create a new vector for the current position
            Vector3D currentPosition = new Vector3D(x, y, z);

            // If movement is negligible, skip check (prevents jitter false positives)
            if (FastMath.areVectorsApproximatelyEqual(positionA, currentPosition)) {
                processing = false;
                return;
            }

            // Otherwise, update positionB and compute movement delta
            positionB = currentPosition;
            Vector3D delta = FastMath.calculateDelta(positionA, positionB); // (Δx, Δy, Δz)

            double deltaX = delta.getX();
            double deltaY = delta.getY();
            double deltaZ = delta.getZ();

            // Determine if Y movement occurred (used to adjust threshold)
            boolean changeInY = Math.abs(deltaY) > 0.000001;

            // Use more lenient threshold if vertical movement suggests jumping or falling
            double threshold = changeInY ? VERTICAL_ADJUSTED_THRESHOLD : DEFAULT_THRESHOLD;

            // Assess max horizontal component of movement
            double horizontalDelta = Math.max(Math.abs(deltaX), Math.abs(deltaZ));

            // Calculate deviation in movement compared to threshold (normalized)
            double speedPercentage = FastMath.calculateSpeedPercentage(horizontalDelta, threshold);
            double speedDeviation = speedPercentage - 100.0;

            // If movement exceeded threshold, flag as a violation
            if (horizontalDelta > threshold) {
                KickMessages.kickPlayerForSpeedCheck(player, String.format("Speed: %+1.2f%%", speedDeviation));
            }

            // Reset position tracking and clear processing flag
            positionA = null;
            positionB = null;
            processing = false;
        });
    }

    /**
     * Prevents further packet processing — typically used during shutdown or teleport.
     */
    public void SpeedCheckAShutdown() {
        processing = true;
    }
}