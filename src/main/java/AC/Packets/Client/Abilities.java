package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsE;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for incoming PLAYER_ABILITIES packets from clients.
 * These packets contain information about the player's ability states (e.g., flying).
 * We use this to detect potentially invalid or malicious behavior (e.g., unauthorized flying).
 */
public class Abilities extends PacketListenerAbstract {

    // Reference to a utility that checks if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store whether a player is an operator, keyed by their UUID.
    // This avoids repeated expensive checks.
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Executor for running validation logic asynchronously to avoid blocking the main thread.
    private final ExecutorService executorService;

    /**
     * Constructor initializes the listener with the highest priority.
     * This ensures we process the packet before other listeners.
     *
     * @param executorService Thread pool for async execution.
     * @param playerOpStorage Utility to check operator status.
     */
    public Abilities(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called automatically when a packet is received from a client.
     * We filter for PLAYER_ABILITIES packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Check if the packet is of type PLAYER_ABILITIES (sent when a player toggles flying, etc.)
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
            handleAbilities(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating the PLAYER_ABILITIES packet.
     * This includes checking if the player is allowed to fly and kicking them if not.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleAbilities(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // We cache this result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data (e.g., isFlying).
        WrapperPlayClientPlayerAbilities abilitiesWrapper = new WrapperPlayClientPlayerAbilities(event);
        boolean isFlying = abilitiesWrapper.isFlying();

        // Run validation asynchronously to avoid lagging the server's main thread.
        executorService.execute(() -> {
            // Validate flying state using custom logic (likely anti-cheat).
            if (!BadPacketsE.isValidFlying(player, isFlying)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "E");
            }
        });
    }
}