package AC.Checks;

import AC.Packets.PacketKind;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class Timer {

    // Configuration constants
    private static final int MAX_TIMESTAMPS = 20;             // Number of packets to collect before analysis
    private static final int REQUIRED_MEANS = 5;              // Number of batch means needed before flagging
    private static final long IGNORE_DELTA = 54L;             // Deltas above this are clamped
    private static final long CLAMPED_DELTA = 50L;            // Clamped value for large deltas
    private static final double AVERAGE_KICK_THRESH = 49.50;  // Threshold to trigger a kick flag

    /**
     * Represents a packet with its timestamp and kind.
     * Used to track timing between packet types.
     */
    private static class TimedPacket {
        final long timestamp;
        final PacketKind kind;

        TimedPacket(long timestamp, PacketKind kind) {
            this.timestamp = timestamp;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return "[" + kind + " @ " + timestamp + "]";
        }
    }

    // Tracks recent packets per player for timing analysis
    private final Map<UUID, LinkedList<TimedPacket>> timestampMap = new ConcurrentHashMap<>();

    // Stores recent batch means per player for averaging
    private final Map<UUID, List<Double>> batchMeansMap = new ConcurrentHashMap<>();

    // Executor for async analysis to avoid blocking main thread
    private final ExecutorService executorService;

    public Timer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Called whenever a packet is received.
     * Buffers the packet and triggers analysis once enough are collected.
     */
    public void recordPacket(Player player,
                             UUID playerUUID,
                             long timestamp,
                             PlayerData playerData,
                             PacketKind kind) {

        // Get or create the packet buffer for this player
        LinkedList<TimedPacket> window =
                timestampMap.computeIfAbsent(playerUUID, id -> new LinkedList<>());

        // Add packet if buffer isn't full
        if (window.size() < MAX_TIMESTAMPS) {
            window.add(new TimedPacket(timestamp, kind));
        }

        // Once buffer is full, snapshot and analyze asynchronously
        if (window.size() == MAX_TIMESTAMPS) {
            List<TimedPacket> snapshot = new ArrayList<>(window);
            executorService.submit(() ->
                    analyzeTimestamps(player, playerUUID, snapshot, playerData)
            );
            window.clear(); // Reset buffer for next batch
        }
    }

    /**
     * Analyzes a batch of packets to detect timer manipulation.
     * Applies ping compensation, delta filtering, clamping, and averaging.
     */
    private void analyzeTimestamps(Player player,
                                   UUID playerUUID,
                                   List<TimedPacket> packets,
                                   PlayerData playerData) {

        long avgPing = (long) playerData.getCurrentPing(); // Ping compensation
        List<Long> validDeltas = new ArrayList<>();

        // Compute deltas between consecutive packets
        for (int i = 1; i < packets.size(); i++) {
            TimedPacket prev = packets.get(i - 1);
            TimedPacket curr = packets.get(i);

            long prevTime = prev.timestamp - avgPing;
            long currTime = curr.timestamp - avgPing;
            long delta = currTime - prevTime;

            // Skip cross-type transitions with small deltas (likely noise)
            if (prev.kind != curr.kind && delta <= 40L) {
                continue;
            }

            // Clamp large deltas to avoid skewing mean
            long accepted = delta > IGNORE_DELTA ? CLAMPED_DELTA : delta;
            validDeltas.add(accepted);
        }

        // If no valid deltas, skip analysis
        if (validDeltas.isEmpty()) {
            return;
        }

        // Establish a minimum delta floor to reduce timer smoothing artifacts
        long maxDelta = validDeltas.stream().mapToLong(Long::longValue).max().orElse(CLAMPED_DELTA);
        long minAllowed = maxDelta - 6;

        // Clamp all deltas below the floor to the floor value
        List<Long> adjustedDeltas = validDeltas.stream()
                .map(d -> d < minAllowed ? minAllowed : d)
                .collect(Collectors.toList());

        // Compute mean of adjusted deltas
        double batchMean = adjustedDeltas.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Track batch means for this player
        List<Double> batchMeans =
                batchMeansMap.computeIfAbsent(playerUUID,
                        id -> Collections.synchronizedList(new ArrayList<>()));

        batchMeans.add(batchMean);

        // Once enough means are collected, compute overall average
        if (batchMeans.size() == REQUIRED_MEANS) {
            double total = batchMeans.stream().mapToDouble(d -> d).sum();
            double averagedMean = total / REQUIRED_MEANS;
            batchMeans.clear(); // Reset for next cycle

            // Estimate timer speed as a percentage of expected tick rate
            double estimatedTimer = (CLAMPED_DELTA / averagedMean) * 100.0;
            estimatedTimer = Math.round(estimatedTimer * 10.0) / 10.0;

            // If timer is running too fast, flag the player
            if (averagedMean < AVERAGE_KICK_THRESH) {
                System.out.println("[Timer] Averaged mean below threshold! Flagging player.");
                KickMessages.broadcastFlagForTimer(player, estimatedTimer);
            }
        }
    }
}