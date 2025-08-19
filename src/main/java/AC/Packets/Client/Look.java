package AC.Packets.Client;

import AC.CLARA;
import AC.Packets.BadPackets.BadPacketsC;
import AC.Packets.PacketKind;
import AC.Utils.CheckUtils.FastMath;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for PLAYER_ROTATION packets sent by clients.
 * These packets are triggered when a player changes their view direction (yaw/pitch).
 * We use this to validate rotation data and detect potential exploits or invalid behavior.
 */
public class Look extends PacketListenerAbstract {

    // Utility to check if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store operator status for each player, keyed by UUID.
    // Prevents repeated permission checks.
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Executor for running validation logic asynchronously.
    private final ExecutorService executorService;

    /**
     * Constructor sets up the listener with highest priority.
     * This ensures our logic runs before other plugins or systems.
     *
     * @param executorService Thread pool for async execution.
     * @param playerOpStorage Utility to check operator status.
     */
    public Look(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PLAYER_ROTATION packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle PLAYER_ROTATION packets (sent when a player moves their camera).
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            handleLook(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating player rotation.
     * This includes normalizing yaw and checking for suspicious values.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleLook(Player player, PacketReceiveEvent event) {
        long ts = System.currentTimeMillis();

        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured rotation data.
        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);

        // Normalize yaw to ensure it's within expected bounds (e.g., -180 to 180 degrees).
        final float normalizedYaw = FastMath.normalizeAngle(wrapper.getYaw());

        // Extract pitch (vertical look angle).
        final float pitch = wrapper.getPitch();

        // Update the wrapper with the normalized yaw value.
        wrapper.setYaw(normalizedYaw);

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the rotation using custom anti-cheat logic.
            if (!BadPacketsC.isValid(normalizedYaw, pitch)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "C");
            }
            CLARA.getInstance()
                    .getTimer()
                    .recordPacket(
                            player,
                            player.getUniqueId(),
                            ts,
                            CLARA.getPlayerData(player.getUniqueId()),
                            PacketKind.LOOK
                    );

        });
    }
}