package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsI;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for PLAYER_BLOCK_PLACEMENT packets sent by clients.
 * These packets are triggered when a player attempts to place a block.
 * We use this to validate placement actions and detect potential exploits or invalid behavior.
 */
public class BlockPlace extends PacketListenerAbstract {

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
    public BlockPlace(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PLAYER_BLOCK_PLACEMENT packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle PLAYER_BLOCK_PLACEMENT packets (sent when a player places a block).
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handleBlockPlace(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating block placement actions.
     * This includes checking the face, cursor position, and placement context.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleBlockPlace(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientPlayerBlockPlacement blockPlacementWrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        // Extract the face ID (numeric representation of the block face being targeted).
        int faceId = blockPlacementWrapper.getFaceId();

        // Extract the block face (e.g., NORTH, UP, DOWN).
        BlockFace face = blockPlacementWrapper.getFace();

        // Extract the cursor position (where the player is aiming on the block surface).
        Vector3f cursorPosition = blockPlacementWrapper.getCursorPosition();

        // Check if the player is placing the block inside another block (can be used for exploit detection).
        boolean insideBlock = blockPlacementWrapper.getInsideBlock().orElse(false);

        // Extract the sequence number (used for tracking packet order).
        int sequence = blockPlacementWrapper.getSequence();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the block placement using custom anti-cheat logic.
            if (!BadPacketsI.isValid(player, faceId, cursorPosition.x, cursorPosition.y, cursorPosition.z, insideBlock, sequence)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "I");
            }
        });
    }
}