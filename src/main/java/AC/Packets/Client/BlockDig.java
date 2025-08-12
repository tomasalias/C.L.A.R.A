package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsJ;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for PLAYER_DIGGING packets sent by clients.
 * These packets are triggered when a player interacts with blocks (e.g., starts breaking, cancels breaking, etc.).
 * We use this to validate digging actions and detect potential exploits or invalid behavior.
 */
public class BlockDig extends PacketListenerAbstract {

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
    public BlockDig(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PLAYER_DIGGING packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle PLAYER_DIGGING packets (sent when a player interacts with blocks).
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handleBlockDig(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating block digging actions.
     * This includes checking the action type, block position, and face.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleBlockDig(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientPlayerDigging blockDiggingWrapper = new WrapperPlayClientPlayerDigging(event);

        // Extract the type of digging action (e.g., START_DESTROY_BLOCK, CANCEL_DESTROY_BLOCK).
        DiggingAction action = blockDiggingWrapper.getAction();

        // Extract the block position being interacted with.
        Vector3i blockPosition = blockDiggingWrapper.getBlockPosition();

        // Extract the face of the block being targeted (e.g., NORTH, UP).
        BlockFace blockFace = blockDiggingWrapper.getBlockFace();

        // Extract the sequence number (used for tracking packet order).
        int sequence = blockDiggingWrapper.getSequence();

        // Break down the block position into individual coordinates.
        int locationX = blockPosition.x;
        int locationY = blockPosition.y;
        int locationZ = blockPosition.z;

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Convert the action to its ordinal value (used for internal validation).
            int actionOrdinal = action != null ? action.ordinal() : -1;

            // Validate the digging action using custom anti-cheat logic.
            if (!BadPacketsJ.isValid(player, locationX, locationY, locationZ, blockFace, action, actionOrdinal)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "J");
            }
        });
    }
}