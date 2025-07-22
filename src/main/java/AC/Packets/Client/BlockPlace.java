package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsI;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPlace extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public BlockPlace(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handleBlockPlace(event.getPlayer(), event);
        }
    }

    private void handleBlockPlace(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators
        }

        WrapperPlayClientPlayerBlockPlacement blockPlacementWrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        int faceId = blockPlacementWrapper.getFaceId();
        BlockFace face = blockPlacementWrapper.getFace();
        Vector3f cursorPosition = blockPlacementWrapper.getCursorPosition();
        boolean insideBlock = blockPlacementWrapper.getInsideBlock().orElse(false);
        int sequence = blockPlacementWrapper.getSequence();

        if (!BadPacketsI.isValid(player, faceId, cursorPosition.x, cursorPosition.y, cursorPosition.z, insideBlock, sequence)) {
            KickMessages.kickPlayerForInvalidPacket(player, "I");
        }
    }
}