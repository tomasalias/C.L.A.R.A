package AC.Checks;

import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class Timer {

    // Max number of timestamps to store per player before analysis
    // This defines the size of the sliding window used to detect abnormal tick rates
    private static final int MAX_TIMESTAMPS = 20;

    // Stores packet receive timestamps per player
    // LinkedList is used for fast append and traversal; ConcurrentHashMap ensures thread safety
    private final Map<UUID, LinkedList<Long>> timestampMap = new ConcurrentHashMap<>();

    // Thread pool for offloading analysis logic
    // This prevents blocking the main thread with math-heavy operations
    private final ExecutorService executorService;

    /**
     * Constructor for Timer check.
     * Accepts a thread pool to offload analysis logic for better performance.
     *
     * @param executorService Shared thread pool from plugin main class
     */
    public Timer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Called from PositionLook when a movement packet is received.
     * This method records the timestamp of the packet for the given player.
     * Once enough timestamps are collected (MAX_TIMESTAMPS), it triggers analysis.
     *
     * @param player      The Bukkit Player object (used for kicking if needed)
     * @param playerUUID  UUID of the player (used as key in timestampMap)
     * @param timestamp   Time the packet was received (System.currentTimeMillis)
     * @param playerData  PlayerData object containing ping and other metadata
     */
    public void recordPacket(Player player, UUID playerUUID, long timestamp, PlayerData playerData) {
        // Retrieve or initialize the timestamp list for this player
        LinkedList<Long> timestamps = timestampMap.computeIfAbsent(playerUUID, id -> new LinkedList<>());

        // Only record if we haven't reached the max
        // This prevents overfilling and ensures consistent batch size for analysis
        if (timestamps.size() < MAX_TIMESTAMPS) {
            timestamps.add(timestamp);
        }

        // Once full, apply logic
        // We analyze the batch and then clear it to prepare for the next window
        if (timestamps.size() == MAX_TIMESTAMPS) {
            // Copy timestamps to avoid mutation issues during async execution
            List<Long> timestampsCopy = new ArrayList<>(timestamps);

            // Offload analysis to thread pool
            // This keeps packet handling fast and avoids blocking the main thread
            executorService.submit(() -> analyzeTimestamps(player, playerUUID, timestampsCopy, playerData));

            timestamps.clear(); // Reset for next batch
        }
    }

    /**
     * Analyzes ping-adjusted deltas between consecutive timestamps.
     * This is the core of the Timer check: it estimates tick intervals and flags suspiciously fast ones.
     *
     * This method is executed asynchronously via the thread pool.
     * It is safe to run off the main thread because:
     * - All math is local and read-only
     * - KickMessages handles thread safety internally
     *
     * @param player      The Bukkit Player object (used for kicking if needed)
     * @param playerUUID  UUID of the player (for logging/debugging)
     * @param timestamps  List of packet timestamps (size == MAX_TIMESTAMPS)
     * @param playerData  PlayerData object containing ping and other metadata
     */
    private void analyzeTimestamps(Player player, UUID playerUUID, List<Long> timestamps, PlayerData playerData) {
        // Retrieve average ping for this player
        // This is subtracted from each timestamp to normalize for latency
        double averagePing = playerData.getCurrentPing();

        // Tracks how many consecutive low deltas we've seen
        // We only kick if we see 3 in a row, to avoid false positives from jitter
        int consecutiveLow = 0;

        // Thresholds for analysis
        // threshold: minimum tick interval (after ping adjustment) considered suspicious
        // maxDelta: upper bound for deltas we consider valid (helps skip outliers)
        final long threshold = 45L;
        final long maxDelta = 55L;

        // Iterate through timestamp pairs to compute deltas
        for (int i = 1; i < timestamps.size(); i++) {
            // Adjust timestamps by subtracting ping
            // This gives us a better estimate of actual client tick timing
            long adjustedPrev = timestamps.get(i - 1) - (long) averagePing;
            long adjustedCurr = timestamps.get(i) - (long) averagePing;
            long delta = adjustedCurr - adjustedPrev;

            // Skip deltas that are too large (likely caused by lag spikes or server hiccups)
            if (delta > maxDelta) {
                continue;
            }

            // If delta is suspiciously low, increment counter
            // If we hit 3 in a row, it's likely a timer cheat
            if (delta <= threshold) {
                consecutiveLow++;
                if (consecutiveLow == 3) {
                    // Kick the player with a predefined message
                    // This message should explain the reason clearly to the player
                    KickMessages.kickPlayerForInvalidPacket(player, "Timer");
                    return;
                }
            } else {
                // Reset counter if delta is valid but not suspicious
                // This ensures we only act on sustained abnormal behavior
                consecutiveLow = 0;
            }
        }
    }
}

// Timer detects abnormal client tick rates by analyzing packet timing.
// It records the timestamps of incoming movement-related packets (e.g., position, position+look),
// then subtracts the player's average ping to estimate their effective tick interval.
//
// Under normal conditions, this interval should hover around 50ms (i.e., 20 ticks per second).
// If the interval is consistently lower—after accounting for latency—it may indicate
// the use of a "timer" cheat, which speeds up the client's game loop to gain an unfair advantage.
//
// This check should include:
// - Smoothing logic to avoid false positives from jitter
// - Configurable thresholds for minimum tick interval
//
// Future improvements:
// - Use exponential moving average for ping smoothing
// - Add a flag for if a PositionPacket is recieved if so run on the data we have collected
// - Integrate with broader violation tracking system for better context
// - Make thresholds configurable via plugin config
// - Add verbose logging toggle for dev environments
// - Add optional rate limiting or batching to avoid thread pool saturation on large servers