package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsH;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for STEER_VEHICLE packets sent by clients.
 * These packets contain directional input for controlling vehicles (e.g., boats, pigs).
 * We use this to validate movement intent and detect potential input manipulation.
 */
public class SteerVehicle extends PacketListenerAbstract {

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
    public SteerVehicle(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for STEER_VEHICLE packets and delegates processing.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // We only care about packets that represent vehicle steering input.
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            handleSteerVehicle(event.getPlayer(), event);
        }
    }

    /**
     * Processes the STEER_VEHICLE packet.
     * Validates directional input and flags suspicious values.
     *
     * @param player The player who sent the packet.
     * @param event  The packet event containing steering data.
     */
    private void handleSteerVehicle(Player player, PacketReceiveEvent event) {
        // Retrieve the player's UUID for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Wrap the raw packet to extract structured steering input.
        WrapperPlayClientSteerVehicle steerVehicleWrapper = new WrapperPlayClientSteerVehicle(event);

        // Extract directional input values and action flags.
        final float forward = steerVehicleWrapper.getForward();
        final float sideways = steerVehicleWrapper.getSideways();
        final boolean jump = steerVehicleWrapper.isJump();
        final boolean unmount = steerVehicleWrapper.isUnmount();

        // Offload validation logic to a background thread to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the steering input using anti-cheat logic.
            // This typically checks for impossible or manipulated values.
            if (!BadPacketsH.isValidSteerMovement(forward, sideways)) {
                // If validation fails, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "H");
            }
        });
    }
}