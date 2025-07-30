package AC.Packets.Client;

// Packet listener for handling incoming player movement packets

import AC.CLARA;
import AC.Checks.Movement.SpeedCheckA;
import AC.Packets.BadPackets.BadPacketsB;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming position packets and performs movement validation
 * including speed checking and packet integrity verification.
 */
public class Position extends PacketListenerAbstract {

    // Map storing movement speed check instance per player UUID
    private final ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap;

    // Utility to determine operator (OP) status for players
    private final PlayerOpStorage playerOpStorage;

    // Cached OP status lookup to avoid repeated permission queries
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    /**
     * Constructor to inject dependencies.
     */
    public Position(ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap, PlayerOpStorage playerOpStorage) {
        this.speedCheckMap = speedCheckMap;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * We only care about position updates for this listener.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            handlePosition(event.getPlayer(), event);
        }
    }

    /**
     * Main logic for handling incoming player position updates.
     * Skips OPs, validates coordinates, triggers speed check and logs position.
     */
    private void handlePosition(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Quick check whether this player is an operator
        // Caches the result for future packets
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Bypass checks for operators
        }

        Vector3d position;
        double x, y, z;
        boolean onGround;

        // Safely extract position data from the packet
        try {
            WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
            position = wrapper.getPosition();
            x = position.getX();
            y = position.getY();
            z = position.getZ();
            onGround = wrapper.isOnGround(); // Useful if vertical checks are later needed
        } catch (Exception e) {
            e.printStackTrace(); // Log decoding failure
            return; // Skip processing for malformed packets
        }

        // Check whether coordinates are valid (not corrupted, NaN, or way outside world bounds)
        try {
            if (!BadPacketsB.isValid(x, y, z)) {
                KickMessages.kickPlayerForInvalidPacket(player, "B"); // Kick with custom reason
                return;
            }

            // If this player has an associated speed check handler, pass the position update along
            SpeedCheckA speedCheckA = speedCheckMap.get(playerUUID);
            if (speedCheckA != null) {
                speedCheckA.handlePosition(player, x, y, z);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error during speed check logic
        }

        long currentTime = System.currentTimeMillis(); // or nanoTime for precision
        CLARA.getInstance().getTimer().onMovementPacket(player.getUniqueId(), currentTime);

        // Log the raw position to playerData for other systems (e.g., outlier detection)
        PlayerData playerData = CLARA.getPlayerData(playerUUID);
        playerData.addPosition(x, y, z);
    }
}