package AC.Packets.Client;

import AC.CLARA;
import AC.Packets.BadPackets.BadPacketsA;
import AC.Packets.PacketKind;
import AC.Utils.CheckUtils.FastMath;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for PLAYER_POSITION_AND_ROTATION packets sent by clients.
 * These packets contain both movement (x, y, z) and orientation (yaw, pitch) data.
 * We use this to validate full positional updates and track timing for anti-cheat checks.
 */
public class PositionLook extends PacketListenerAbstract {

    // Utility to check if a player is an operator
    private final PlayerOpStorage playerOpStorage;

    // Cache to store operator status per player UUID for performance
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Thread pool for executing validation logic asynchronously
    private final ExecutorService executorService;

    /**
     * Constructor sets up the listener with highest priority and required services.
     *
     * @param executorService Thread pool for async execution.
     * @param playerOpStorage Utility to check operator status.
     */
    public PositionLook(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PLAYER_POSITION_AND_ROTATION packets and delegates processing.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // We only care about packets that contain both position and rotation data.
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndLook(event.getPlayer(), event);
        }
    }

    /**
     * Processes the PLAYER_POSITION_AND_ROTATION packet.
     * Validates movement and orientation values, and updates timing for anti-cheat checks.
     *
     * @param player The player who sent the packet.
     * @param event  The packet event containing position and rotation data.
     */
    private void handlePositionAndLook(Player player, PacketReceiveEvent event) {
        long ts = System.currentTimeMillis();
        // Retrieve the player's UUID for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Attempt to wrap the raw packet into a structured format to extract movement and rotation data.
        // If the packet is malformed or wrapping fails, log the error and skip processing.
        WrapperPlayClientPlayerPositionAndRotation wrapper;
        try {
            wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Extract movement coordinates and orientation angles from the packet.
        final double x = wrapper.getPosition().getX();
        final double y = wrapper.getPosition().getY();
        final double z = wrapper.getPosition().getZ();
        final float yaw = wrapper.getYaw();
        final float pitch = wrapper.getPitch();
        final boolean onGround = wrapper.isOnGround();

        // Normalize yaw to ensure it's within expected bounds (e.g., -180 to 180 degrees).
        final float normalizedYaw = FastMath.normalizeAngle(yaw);
        // Update the wrapper with the normalized yaw value.
        wrapper.setYaw(normalizedYaw);

        // Offload validation and timing logic to a background thread to avoid blocking the main server thread.
        executorService.execute(() -> {
            try {
                // Validate the coordinates and rotation using anti-cheat logic.
                // This typically checks for invalid values like NaN, infinity, or extreme out-of-bounds inputs.
                if (!BadPacketsA.isValid(player, x, y, z, normalizedYaw, pitch)) {
                    // If validation fails, kick the player with a predefined message.
                    KickMessages.kickPlayerForInvalidPacket(player, "A");
                    return;
                }

                CLARA.getInstance()
                        .getTimer()
                        .recordPacket(
                                player,
                                player.getUniqueId(),
                                ts,
                                CLARA.getPlayerData(player.getUniqueId()),
                                PacketKind.POSITION_AND_ROTATION
                        );


            } catch (Exception e) {
                // If validation throws an exception, log it and skip further processing.
                e.printStackTrace();
            }
        });
    }
}