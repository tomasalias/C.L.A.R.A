package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsG;
import AC.Utils.CheckUtils.FastMath;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This class listens for VEHICLE_MOVE packets sent by clients.
 * These packets contain movement data for entities the player is riding (e.g., boats, minecarts).
 * We use this to validate vehicle movement and detect suspicious behavior.
 */
public class VehicleMove extends PacketListenerAbstract {

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
    public VehicleMove(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for VEHICLE_MOVE packets and delegates processing.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // We only care about packets that represent movement of vehicles the player is riding.
        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            handleVehicleMove(event.getPlayer(), event);
        }
    }

    /**
     * Processes the VEHICLE_MOVE packet.
     * Validates movement coordinates and orientation, and flags suspicious values.
     *
     * @param player The player who sent the packet.
     * @param event  The packet event containing vehicle movement data.
     */
    private void handleVehicleMove(Player player, PacketReceiveEvent event) {
        // Retrieve the player's UUID for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Wrap the raw packet to extract structured vehicle movement data.
        WrapperPlayClientVehicleMove vehicleMoveWrapper = new WrapperPlayClientVehicleMove(event);

        // Extract movement coordinates and orientation angles from the packet.
        final double x = vehicleMoveWrapper.getPosition().getX();
        final double y = vehicleMoveWrapper.getPosition().getY();
        final double z = vehicleMoveWrapper.getPosition().getZ();
        final float yaw = vehicleMoveWrapper.getYaw();
        final float pitch = vehicleMoveWrapper.getPitch();
        // Normalize yaw to ensure it's within expected bounds (e.g., -180 to 180 degrees).
        final float normalizedYaw = FastMath.normalizeAngle(vehicleMoveWrapper.getYaw());
        // Update the wrapper with the normalized yaw value.
        vehicleMoveWrapper.setYaw(normalizedYaw);


        System.out.println("[VEHICLE_MOVE DEBUG] Player: " + player.getName());
        System.out.println("  Position: X=" + x + ", Y=" + y + ", Z=" + z);
        System.out.println("  Rotation: Yaw=" + normalizedYaw + ", Pitch=" + pitch);


        // Offload validation logic to a background thread to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the vehicle movement using anti-cheat logic.
            // This typically checks for impossible or manipulated values.
            if (!BadPacketsG.isValid(x, y, z, normalizedYaw, pitch)) {
                // If validation fails, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "G");
            }
        });
    }
}