package AC.Checks;

import AC.Packets.PacketKind;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.CheckUtils.FastMath;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Timer {

    // Configuration constants
    private static final int MAX_TIMESTAMPS = 20;
    private static final int REQUIRED_MEANS = 5;
    private static final double AVERAGE_KICK_THRESH = 49.8;

    // Debug toggle for verbose logging
    private static final boolean DEBUG = true;

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

    private final Map<UUID, LinkedList<TimedPacket>> timestampMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<Double>> batchMeansMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public Timer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void recordPacket(Player player,
                             UUID playerUUID,
                             long timestamp,
                             PlayerData playerData,
                             PacketKind kind) {

        LinkedList<TimedPacket> window =
                timestampMap.computeIfAbsent(playerUUID, id -> new LinkedList<>());

        if (window.size() < MAX_TIMESTAMPS) {
            window.add(new TimedPacket(timestamp, kind));
        }

        if (window.size() == MAX_TIMESTAMPS) {
            List<TimedPacket> snapshot = new ArrayList<>(window);
            executorService.submit(() ->
                    analyzeTimestamps(player, playerUUID, snapshot, playerData)
            );
            window.clear();
        }
    }

    private void analyzeTimestamps(Player player,
                                   UUID playerUUID,
                                   List<TimedPacket> packets,
                                   PlayerData playerData) {

        long avgPing = (long) playerData.getCurrentPing();
        List<Double> smoothedDeltas = new ArrayList<>();

        FastMath.KalmanFilter filter = new FastMath.KalmanFilter(50.0, 0.5, 0.2, 2.0);

        for (int i = 1; i < packets.size(); i++) {
            TimedPacket prev = packets.get(i - 1);
            TimedPacket curr = packets.get(i);

            long prevTime = prev.timestamp - avgPing;
            long currTime = curr.timestamp - avgPing;
            long delta = currTime - prevTime;

            if (prev.kind != curr.kind && delta <= 40L) {
                continue;
            }

            if (delta > 60L) {
                filter.reset(50.0, 0.5);
            }

            double smoothed = filter.update(delta);
            smoothedDeltas.add(smoothed);
        }

        if (smoothedDeltas.isEmpty()) {
            return;
        }

        double batchMean = smoothedDeltas.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        if (batchMean <= 52.0) {
            List<Double> batchMeans =
                    batchMeansMap.computeIfAbsent(playerUUID,
                            id -> Collections.synchronizedList(new ArrayList<>()));

            batchMeans.add(batchMean);

            if (batchMeans.size() == REQUIRED_MEANS) {
                double total = batchMeans.stream().mapToDouble(d -> d).sum();
                double averagedMean = total / REQUIRED_MEANS;
                batchMeans.clear();

                double estimatedTimer = (50.0 / averagedMean) * 100.0;
                estimatedTimer = Math.round(estimatedTimer * 10.0) / 10.0;

                System.out.println("[Timer][Final] Averaged mean: " + String.format("%.3f", averagedMean));
                System.out.println("[Timer][Final] Estimated timer speed: " + estimatedTimer + "%");

                if (averagedMean < AVERAGE_KICK_THRESH) {
                    KickMessages.broadcastFlagForTimer(player, estimatedTimer);
                }
            }
        }
    }
}