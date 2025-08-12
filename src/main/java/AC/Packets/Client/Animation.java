package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsL;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for SWING packets sent by clients.
 * These packets are triggered when a player swings their arm (e.g., attacking or interacting).
 * We use this to validate hand usage and detect potential exploits or invalid behavior.
 */
public class Animation extends PacketListenerAbstract {

    // Utility to check if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store operator status for each player, keyed by UUID.
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
    public Animation(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for SWING packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            handleSwingPacket(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating swing hand packets.
     * This includes checking if the hand value is valid (typically MAIN_HAND or OFF_HAND).
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleSwingPacket(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientAnimation swingWrapper = new WrapperPlayClientAnimation(event);

        // Extract the hand used in the swing (MAIN_HAND or OFF_HAND).
        int handOrdinal = swingWrapper.getHand().ordinal();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the hand value using custom anti-cheat logic.
            // Valid ordinals are typically 0 (MAIN_HAND) and 1 (OFF_HAND).
            if (!BadPacketsL.isValid(handOrdinal)) {
                KickMessages.kickPlayerForInvalidPacket(player, "F");
            }
        });
    }
}