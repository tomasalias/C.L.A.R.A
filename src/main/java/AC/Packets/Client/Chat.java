package AC.Packets.Client;

import AC.CLARA;
import AC.Packets.BadPackets.BadPacketsD;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Chat extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActionTimes = new ConcurrentHashMap<>();
    private final long COOLDOWN_TIME = 50L;

    public Chat(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            handleChat(event.getPlayer(), event);
        }
    }

    private void handleChat(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip filtering for operators
        }

        WrapperPlayClientChatMessage chatWrapper = new WrapperPlayClientChatMessage(event);
        String message = chatWrapper.getMessage();

        long currentTime = System.currentTimeMillis();
        long lastActionTime = lastActionTimes.getOrDefault(playerUUID, 0L);

        if (!BadPacketsD.isValid(message)) {
            event.setCancelled(true);

            if (currentTime - lastActionTime >= COOLDOWN_TIME) {
                Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
                    player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Moderator] C.L.A.R.A: " +
                            ChatColor.RED + "Please refrain from using offensive terms.");
                });
                lastActionTimes.put(playerUUID, currentTime);
            }
            return;
        }

        String formattedMessage = "<" + player.getName() + "> " + message;
        event.setCancelled(true);

        if (currentTime - lastActionTime >= COOLDOWN_TIME) {
            Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
                Bukkit.broadcastMessage(formattedMessage);
            });
            lastActionTimes.put(playerUUID, currentTime);
        }
    }
}