package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsH;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SteerVehicle extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public SteerVehicle(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            handleSteerVehicle(event.getPlayer(), event);
        }
    }

    private void handleSteerVehicle(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip enforcement for operators
        }

        WrapperPlayClientSteerVehicle steerVehicleWrapper = new WrapperPlayClientSteerVehicle(event);
        float forward = steerVehicleWrapper.getForward();
        float sideways = steerVehicleWrapper.getSideways();
        boolean jump = steerVehicleWrapper.isJump();
        boolean unmount = steerVehicleWrapper.isUnmount();

        if (!BadPacketsH.isValidSteerMovement(forward, sideways)) {
            KickMessages.kickPlayerForInvalidPacket(player, "H");
        }
    }
}