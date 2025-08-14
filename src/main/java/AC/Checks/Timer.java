package AC.Checks;

import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Timer check to detect abnormal packet rates (timer cheats).
 *
 * This class:
 *  - Records timestamps of movement packets in sliding windows.
 *  - Computes ping-normalized deltas between successive packets.
 *  - Filters out lag spikes and calculates the mean delta per window.
 *  - Stores each window's mean separately.
 *  - Once 5 means are collected, averages them and checks against threshold.
 *  - Triggers a flag/kick only if the averaged mean is below threshold.
 */
public class Timer {

    // Number of timestamps to collect before analysis (defines window size)
    private static final int MAX_TIMESTAMPS = 20;

    // Number of batch means to collect before evaluating
    private static final int REQUIRED_MEANS = 5;

    // If a ping-adjusted delta exceeds this, treat it as a lag spike and skip it
    private static final long IGNORE_DELTA = 60L;

    // If the averaged mean of 5 batches falls below this, we consider it cheating
    private static final double AVERAGE_KICK_THRESHOLD = 49.50;

    // Thread-safe map: player UUID → list of raw packet timestamps
    private final Map<UUID, LinkedList<Long>> timestampMap = new ConcurrentHashMap<>();

    // Thread-safe map: player UUID → list of recent batch means
    private final Map<UUID, List<Double>> batchMeansMap = new ConcurrentHashMap<>();

    // Shared thread pool for offloading analysis off the main server thread
    private final ExecutorService executorService;

    // Debug mode toggle: enables/disables print statements
    private boolean debugMode = false;

    /**
     * Constructor.
     *
     * @param executorService Shared ExecutorService from plugin main class
     */
    public Timer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Enable or disable debug mode.
     *
     * @param enabled true to enable debug output, false to disable
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    /**
     * Called whenever a movement packet arrives for a player.
     *
     * We record the packet's arrival time in a per-player list.
     * Once we have MAX_TIMESTAMPS entries, we snapshot & clear the list,
     * then offload the analysis to the thread pool to keep tick handling fast.
     *
     * @param player      Bukkit Player instance (used for kicking/flagging)
     * @param playerUUID  Unique ID for indexing our maps
     * @param timestamp   System.currentTimeMillis() of packet receipt
     * @param playerData  Contains metadata like current ping
     */
    public void recordPacket(Player player, UUID playerUUID, long timestamp, PlayerData playerData) {
        LinkedList<Long> timestamps = timestampMap
                .computeIfAbsent(playerUUID, id -> new LinkedList<>());

        if (timestamps.size() < MAX_TIMESTAMPS) {
            timestamps.add(timestamp);
        }

        if (timestamps.size() == MAX_TIMESTAMPS) {
            List<Long> snapshot = new ArrayList<>(timestamps);
            executorService.submit(() ->
                    analyzeTimestamps(player, playerUUID, snapshot, playerData)
            );
            timestamps.clear();
        }
    }

    /**
     * Analyze a batch of timestamps to determine if the player is cheating.
     *
     * Steps:
     *  1. Normalize timestamps by subtracting average ping.
     *  2. Compute deltas between each consecutive pair.
     *  3. Filter out any delta > IGNORE_DELTA (lag spikes).
     *  4. Calculate the mean of the remaining deltas.
     *  5. Store the batch mean in a per-player buffer.
     *  6. Once 5 batch means are collected, average them.
     *  7. If the averaged mean < AVERAGE_KICK_THRESHOLD → broadcast a flag/kick.
     *
     * Thread Safety:
     *  - All local data (lists, variables) is confined to this method.
     *  - batchMeansMap uses synchronized lists via ConcurrentHashMap.
     *  - KickMessages is assumed to handle any additional synchronization.
     *
     * @param player      Bukkit Player instance
     * @param playerUUID  Player’s UUID (key in our history maps)
     * @param timestamps  Exactly MAX_TIMESTAMPS entries of raw System.currentTimeMillis()
     * @param playerData  Provides getCurrentPing() for latency normalization
     */
    private void analyzeTimestamps(Player player,
                                   UUID playerUUID,
                                   List<Long> timestamps,
                                   PlayerData playerData) {
        long averagePing = (long) playerData.getCurrentPing();
        List<Long> validDeltas = new ArrayList<>();

        for (int i = 1; i < timestamps.size(); i++) {
            long prev = timestamps.get(i - 1) - averagePing;
            long current = timestamps.get(i) - averagePing;
            long delta = current - prev;

            if (debugMode) {
                System.out.println("[Timer] Raw delta: " + delta);
            }

            if (delta <= IGNORE_DELTA) {
                validDeltas.add(delta);
            }
        }

        if (validDeltas.isEmpty()) {
            return;
        }

        long sum = 0;
        for (long d : validDeltas) {
            sum += d;
        }
        double batchMean = (double) sum / validDeltas.size();

        if (debugMode) {
            System.out.println("[Timer] Batch mean: " + batchMean);
        }

        List<Double> batchMeans = batchMeansMap
                .computeIfAbsent(playerUUID, id -> new ArrayList<>());
        batchMeans.add(batchMean);

        if (batchMeans.size() == REQUIRED_MEANS) {
            double total = 0;
            for (double m : batchMeans) {
                total += m;
            }
            double averagedMean = total / REQUIRED_MEANS;

            if (debugMode) {
                System.out.println("[Timer] Averaged mean of 5 batches: " + averagedMean);
            }

            batchMeans.clear();

            if (averagedMean < AVERAGE_KICK_THRESHOLD) {
                KickMessages.broadcastFlagForTimer(player, averagedMean);
            }
        }
    }
}