package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsF;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for HELD_ITEM_CHANGE packets sent by clients.
 * These packets are triggered when a player switches their hotbar slot.
 * We use this to validate slot changes and detect potential exploits or invalid behavior.
 */
public class HeldItemSlot extends PacketListenerAbstract {

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
    public HeldItemSlot(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for HELD_ITEM_CHANGE packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle HELD_ITEM_CHANGE packets (sent when a player switches hotbar slots).
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            handleHeldItemSlot(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating held item slot changes.
     * This includes checking if the selected slot is within valid bounds.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleHeldItemSlot(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientHeldItemChange heldItemWrapper = new WrapperPlayClientHeldItemChange(event);

        // Extract the hotbar slot index the player is switching to.
        int slot = heldItemWrapper.getSlot();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the slot index using custom anti-cheat logic.
            // Typically, valid slots are 0–8 (standard hotbar range).
            if (!BadPacketsF.isValid(slot)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "F");
            }
        });
    }
}