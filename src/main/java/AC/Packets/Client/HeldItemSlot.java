package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsF;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeldItemSlot extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public HeldItemSlot(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            handleHeldItemSlot(event.getPlayer(), event);
        }
    }

    private void handleHeldItemSlot(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators
        }

        WrapperPlayClientHeldItemChange heldItemWrapper = new WrapperPlayClientHeldItemChange(event);
        int slot = heldItemWrapper.getSlot();

        if (!BadPacketsF.isValid(slot)) {
            KickMessages.kickPlayerForInvalidPacket(player, "F");
        }
    }
}