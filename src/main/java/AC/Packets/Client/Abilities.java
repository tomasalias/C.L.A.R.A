package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsE;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Abilities extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;

    // Optional: Cache player OP status for efficiency (like in SpeedCheckA)
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public Abilities(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
            handleAbilities(event.getPlayer(), event);
        }
    }

    private void handleAbilities(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check and cache OP status
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip check for operators
        }

        WrapperPlayClientPlayerAbilities abilitiesWrapper = new WrapperPlayClientPlayerAbilities(event);
        boolean isFlying = abilitiesWrapper.isFlying();

        if (!BadPacketsE.isValidFlying(player, isFlying)) {
            KickMessages.kickPlayerForInvalidPacket(player, "E");
        }
    }
}