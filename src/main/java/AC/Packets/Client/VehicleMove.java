package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsG;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleMove extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public VehicleMove(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            handleVehicleMove(event.getPlayer(), event);
        }
    }

    private void handleVehicleMove(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip packet enforcement for operators
        }

        WrapperPlayClientVehicleMove vehicleMoveWrapper = new WrapperPlayClientVehicleMove(event);
        Vector3d position = vehicleMoveWrapper.getPosition();
        float yaw = vehicleMoveWrapper.getYaw();
        float pitch = vehicleMoveWrapper.getPitch();

        if (!BadPacketsG.isValid(position.getX(), position.getY(), position.getZ(), yaw, pitch)) {
            KickMessages.kickPlayerForInvalidPacket(player, "G");
        }
    }
}