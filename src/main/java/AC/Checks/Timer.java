package AC.Checks;

import AC.CLARA;
import AC.Utils.CheckUtils.PlayerData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-player packet timing tracker.
 * Logs the time interval between consecutive movement packets.
 */
public class Timer {

    // Stores last packet receive timestamp per player
    private final ConcurrentHashMap<UUID, Long> lastPacketTimestamps = new ConcurrentHashMap<>();

    /**
     * Called when a packet is received for a given player.
     * @param playerId Player UUID
     * @param currentTime Time packet was received (in ms)
     */
    public void onMovementPacket(UUID playerId, long currentTime) {
        Long previousTime = lastPacketTimestamps.get(playerId);

        if (previousTime != null) {
            long delta = currentTime - previousTime;

            // Grab player ping from their PlayerData
            PlayerData playerData = CLARA.getPlayerData(playerId);
            long playerPing = (long) playerData.getCurrentPing(); // Truncated to long if it's averaged

            long deviation = delta - 50 - playerPing;
        }

        lastPacketTimestamps.put(playerId, currentTime);
    }
}