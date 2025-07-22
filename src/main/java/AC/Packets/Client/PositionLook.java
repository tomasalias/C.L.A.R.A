package AC.Packets.Client;

import AC.CLARA;
import AC.Checks.Movement.SpeedCheckA;
import AC.Packets.BadPackets.BadPacketsB;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PositionLook extends PacketListenerAbstract {

    private final ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap;
    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public PositionLook(ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap, PlayerOpStorage playerOpStorage) {
        this.speedCheckMap = speedCheckMap;
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndLook(event.getPlayer(), event);
        }
    }

    private void handlePositionAndLook(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators
        }

        Vector3d position;
        double x, y, z;
        float yaw, pitch;
        boolean onGround;

        try {
            WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            position = wrapper.getPosition();
            x = position.getX();
            y = position.getY();
            z = position.getZ();
            yaw = wrapper.getYaw();
            pitch = wrapper.getPitch();
            onGround = wrapper.isOnGround();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            if (!BadPacketsB.isValid(x, y, z)) {
                KickMessages.kickPlayerForInvalidPacket(player, "B");
                return;
            }

            SpeedCheckA speedCheckA = speedCheckMap.get(playerUUID);
            if (speedCheckA != null) {
                speedCheckA.handlePosition(player, x, y, z);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        PlayerData playerData = CLARA.getPlayerData(playerUUID);
        playerData.addPosition(x, y, z);
    }
}