package AC.Packets.Client;

import AC.CLARA;
import AC.Checks.Movement.VelocityCheckA;
import AC.Packets.BadPackets.BadPacketsB;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for PLAYER_POSITION packets sent by clients.
 * These packets contain movement data (x, y, z, onGround).
 * We use this to validate movement and track timing for checks.
 */
public class Position extends PacketListenerAbstract {

    // Stores operator status to avoid repeated permission checks
    private final PlayerOpStorage playerOpStorage;

    // Thread-safe cache mapping player UUIDs to their operator status
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Executor for async processing to avoid blocking the main thread
    private final ExecutorService executorService;

    /**
     * Constructor initializes listener with HIGHEST priority and required services.
     *
     * @param executorService Thread pool for async execution.
     * @param playerOpStorage Utility to check if a player is an operator.
     */
    public Position(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST); // Ensures this listener runs before others
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PLAYER_POSITION packets and processes them.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // We only care about PLAYER_POSITION packets, which contain movement data.
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            // Delegate the handling of movement logic to a dedicated method.
            handlePosition(event.getPlayer(), event);
        }
    }

    /**
     * Handles the PLAYER_POSITION packet.
     * Validates movement coordinates and updates movement timing.
     *
     * @param player The player who sent the movement packet.
     * @param event  The packet event containing position data.
     */
    private void handlePosition(Player player, PacketReceiveEvent event) {
        // Retrieve the player's unique identifier for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. We cache this result to avoid repeated permission checks.
        // If the cache doesn't contain the UUID, we query the PlayerOpStorage and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing for them.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Attempt to wrap the raw packet into a structured format to extract movement data.
        // If the packet is malformed or wrapping fails, we log the error and skip processing.
        WrapperPlayClientPlayerPosition wrapper;
        try {
            wrapper = new WrapperPlayClientPlayerPosition(event);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Extract the movement coordinates (x, y, z) and the onGround flag from the packet.
        final double x = wrapper.getPosition().getX();
        final double y = wrapper.getPosition().getY();
        final double z = wrapper.getPosition().getZ();
        final boolean onGround = wrapper.isOnGround();

    }
}