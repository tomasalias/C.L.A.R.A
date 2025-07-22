package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsC;
import AC.Utils.CheckUtils.FastMath;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Look extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public Look(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            handleLook(event.getPlayer(), event);
        }
    }

    private void handleLook(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip enforcement for operators
        }

        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();
        boolean onGround = wrapper.isOnGround(); // Currently unused

        yaw = FastMath.normalizeAngle(yaw);
        wrapper.setYaw(yaw);

        if (!BadPacketsC.isValid(yaw, pitch)) {
            KickMessages.kickPlayerForInvalidPacket(player, "C");
        }
    }
}