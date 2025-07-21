package AC.Checks.Movement;

import AC.Utils.CheckUtils.FastMath;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class SpeedCheckA {
    // Maximum difference thresholds for detecting speed based on Y-axis change
    private static final double MAX_DIFFERENCE_THRESHOLD_Y_CHANGED = 1.14;
    private static final double MAX_DIFFERENCE_THRESHOLD_Y_SAME = 0.287;
    private static final double EPSILON = 1e-5; // Small epsilon value to avoid floating point precision issues

    @Getter
    private final UUID playerUUID; // The unique identifier of the player being checked
    private Vector3D positionA = null; // Initial position, before movement is detected
    private Vector3D positionB = null; // Subsequent position after movement
    private boolean processing = false; // Flag to prevent concurrent execution of checks
    private int deltaYCounter = -1; // Tracks the Y-axis delta count (initially not calculated)
    private final ExecutorService executorService; // Executor service for handling tasks asynchronously

    // Cached OP status for the player, helps avoid repetitive checks for the same player
    private Boolean isOpCached = null; // null means the OP status hasn't been cached yet

    private final PlayerOpStorage playerOpStorage; // Declare the field

    public SpeedCheckA(UUID playerUUID, PlayerOpStorage playerOpStorage, ExecutorService executorService) {
        this.playerUUID = playerUUID;
        this.playerOpStorage = playerOpStorage; // Store the passed PlayerOpStorage instance
        this.executorService = executorService;
    }

    // Handles the player's position updates and checks if they are moving suspiciously fast
    public void handlePosition(Player player, double x, double y, double z) {
        // Cache the player's OP status the first time it is needed to avoid redundant checks
        // Cache the player's OP status the first time it is needed to avoid redundant checks
        if (isOpCached == null) {

            isOpCached = playerOpStorage.isPlayerOperator(player); // Fetch OP status via the storage utility
        }

        // If the player is an operator (OP), we skip the speed check to avoid false positives
        if (isOpCached) {
            return; // If OP, bypass speed checks
        }

        // Ensure that speed checks aren't processed concurrently for the same player
        if (processing) {
            return; // If already processing, skip the current check
        }

        // Set the flag to indicate we are processing the player's movement
        processing = true;

        // Use the ExecutorService to run the speed check in a separate thread, to avoid blocking the main thread
        executorService.submit(() -> {
            // Increment the deltaYCounter after each position update (unless it is reset)
            if (deltaYCounter != -1) {
                deltaYCounter++;
            }

            // If this is the first position check, we set positionA and return to avoid further processing
            if (positionA == null) {
                positionA = new Vector3D(x, y, z); // Save the initial position
                processing = false; // Mark the processing as complete
                return;
            }

            // Check if the new position is the same as the initial one. If so, skip the check
            if (FastMath.areVectorsApproximatelyEqual(positionA, new Vector3D(x, y, z))) {
                processing = false; // Mark processing as complete
                return;
            }

            // Store the new position as positionB
            positionB = new Vector3D(x, y, z);

            // Calculate the movement delta between positionA and positionB (change in X, Y, and Z)
            Vector3D delta = FastMath.calculateDelta(positionA, positionB);
            double deltaX = delta.getX();
            double deltaY = delta.getY();
            double deltaZ = delta.getZ();

            // If a significant change in Y position is detected (deltaY > EPSILON), reset the deltaYCounter
            if (deltaY > EPSILON) {
                deltaYCounter = 0; // Reset counter for deltaY
            }

            // Select a threshold for speed check based on the deltaYCounter:
            double threshold = (deltaYCounter != -1 && deltaYCounter <= 11)
                    ? MAX_DIFFERENCE_THRESHOLD_Y_CHANGED
                    : MAX_DIFFERENCE_THRESHOLD_Y_SAME;

            // Find the highest delta (X, Y, Z) to calculate the speed
            double maxDelta = Math.max(Math.max(deltaX, deltaY), deltaZ);

            // Calculate the speed percentage based on the threshold and max delta
            double speedPercentage = FastMath.calculateSpeedPercentage(maxDelta, threshold);

            // Calculate the percentage difference from 100% (speed deviation)
            double speedDeviation = speedPercentage - 100.0;

            // If the player's speed exceeds the threshold, flag them
            if (maxDelta > threshold) {
                // Log the kick details and trigger a speed violation kick
                logKickDetails(player, x, y, z, deltaX, deltaY, deltaZ, threshold, deltaYCounter, speedDeviation);
                KickMessages.kickPlayerForSpeedCheck(player, String.format("Speed: %+1.2f%%", speedDeviation)); // Kick with the percentage deviation
            }

            // Reset positionA and positionB for the next movement check
            positionA = null;
            positionB = null;
            processing = false; // Mark processing as complete
        });
    }

    // Logs detailed information when a player is flagged for speeding
    private void logKickDetails(Player player, double x, double y, double z,
                                double deltaX, double deltaY, double deltaZ,
                                double threshold, int deltaYCounter, double speedDeviation) {
        // Detailed logging for debugging and monitoring
        System.out.println("[SpeedCheckA] Logging kick details...");
        Bukkit.getLogger().info("[SpeedCheckA] Kicking player for speed hacking!");
        Bukkit.getLogger().info("[SpeedCheckA] Player: " + player.getName());
        Bukkit.getLogger().info("[SpeedCheckA] Player UUID: " + playerUUID.toString()); // Log UUID for identification
        Bukkit.getLogger().info("[SpeedCheckA] Position A: x=" + positionA.getX() + ", y=" + positionA.getY() + ", z=" + positionA.getZ());
        Bukkit.getLogger().info("[SpeedCheckA] Position B: x=" + x + ", y=" + y + ", z=" + z);
        Bukkit.getLogger().info("[SpeedCheckA] Delta X: " + deltaX); // X-axis movement delta
        Bukkit.getLogger().info("[SpeedCheckA] Delta Y: " + deltaY); // Y-axis movement delta
        Bukkit.getLogger().info("[SpeedCheckA] Delta Z: " + deltaZ); // Z-axis movement delta
        Bukkit.getLogger().info("[SpeedCheckA] Threshold: " + threshold); // Threshold for flagging
        Bukkit.getLogger().info("[SpeedCheckA] DeltaY Counter: " + deltaYCounter); // How many cycles since Y delta was significant
        Bukkit.getLogger().info("[SpeedCheckA] Speed Deviation: " + speedDeviation + "%"); // Speed deviation
    }

    // Stops the speed check processing, likely used during plugin shutdown or cleanup
    public void SpeedCheckAShutdown() {
        processing = true; // Mark processing as complete to indicate shutdown state
    }
}
