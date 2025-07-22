package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsJ;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
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

public class BlockDig extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public BlockDig(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handleBlockDig(event.getPlayer(), event);
        }
    }

    private void handleBlockDig(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators
        }

        WrapperPlayClientPlayerDigging blockDiggingWrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = blockDiggingWrapper.getAction();
        Vector3i blockPosition = blockDiggingWrapper.getBlockPosition();
        BlockFace blockFace = blockDiggingWrapper.getBlockFace();
        int sequence = blockDiggingWrapper.getSequence();

        int locationX = blockPosition.x;
        int locationY = blockPosition.y;
        int locationZ = blockPosition.z;

        if (!BadPacketsJ.isValid(player, locationX, locationY, locationZ, blockFace, action, action != null ? action.ordinal() : -1)) {
            KickMessages.kickPlayerForInvalidPacket(player, "J");
        }
    }
}