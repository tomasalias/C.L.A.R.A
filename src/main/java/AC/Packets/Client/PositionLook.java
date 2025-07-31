package AC.Packets.Client;

// Packet listener for processing incoming position + look (rotation) packets

import AC.CLARA;
import AC.Checks.Movement.SpeedCheckA;
import AC.Checks.Movement.VelocityCheckA;
import AC.Packets.BadPackets.BadPacketsB;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.CheckUtils.VelocityCheckStorage;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming PLAYER_POSITION_AND_ROTATION packets.
 * Applies validation logic, forwards movement data to SpeedCheckA, and logs coordinates to PlayerData.
 */
public class PositionLook extends PacketListenerAbstract {

    // Active speed check module per player
    private final ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap;

    // Utility for checking operator status
    private final PlayerOpStorage playerOpStorage;

    // OP status cache to reduce permission lookups
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    /**
     * Constructor for injection of movement check and operator storage dependencies.
     */
    public PositionLook(ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap, PlayerOpStorage playerOpStorage) {
        this.speedCheckMap = speedCheckMap;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Packet dispatcher. Filters for PLAYER_POSITION_AND_ROTATION packets only.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndLook(event.getPlayer(), event);
        }
    }

    /**
     * Main logic for decoding player movement and rotation.
     * Skips operators, validates coordinates, pipes valid movement to SpeedCheckA, logs to PlayerData.
     */
    private void handlePositionAndLook(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Compute and cache operator status if not already cached
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators
        }

        // Movement data containers
        Vector3d position;
        double x, y, z;
        float yaw, pitch;
        boolean onGround;

        // Safely unpack position and rotation fields from the packet
        try {
            WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            position = wrapper.getPosition();
            x = position.getX();
            y = position.getY();
            z = position.getZ();
            yaw = wrapper.getYaw();      // Not currently used but can be useful for future look validation
            pitch = wrapper.getPitch();  // Ditto for pitch
            onGround = wrapper.isOnGround(); // For future fall validation or vertical checks
        } catch (Exception e) {
            e.printStackTrace(); // Log decoding failure
            return;
        }

        // Validate the incoming position data
        try {
            if (!BadPacketsB.isValid(x, y, z)) {
                KickMessages.kickPlayerForInvalidPacket(player, "B"); // Kick for malformed or suspect packet
                return;
            }

            // If a speed check exists for this player, route the position to it
            SpeedCheckA speedCheckA = speedCheckMap.get(playerUUID);
            if (speedCheckA != null) {
                speedCheckA.handlePosition(player, x, y, z);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Fail safely and log diagnostic info
        }
        long currentTime = System.currentTimeMillis(); // or nanoTime for precision
        CLARA.getInstance().getTimer().onMovementPacket(player.getUniqueId(), currentTime);

        // Log raw position data for auxiliary systems (e.g., packet history or outlier detection)
        PlayerData playerData = CLARA.getPlayerData(playerUUID);
        playerData.addPosition(x, y, z);

        VelocityCheckA velocityCheck = VelocityCheckStorage.get(playerUUID);
        if (velocityCheck != null) {
            velocityCheck.addPosition(x, y, z);
        }

    }
}