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

/**
 * This class listens for CHAT_MESSAGE packets sent by clients.
 * These packets are triggered when a player sends a chat message.
 * We use this to validate message content and enforce chat moderation rules.
 */
public class Chat extends PacketListenerAbstract {

    // Utility to check if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store operator status for each player, keyed by UUID.
    // Prevents repeated permission checks.
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Stores the last time a player triggered a chat-related action.
    // Used to enforce cooldowns and prevent spam.
    private final Map<UUID, Long> lastActionTimes = new ConcurrentHashMap<>();

    // Cooldown time in milliseconds between chat actions (e.g., warnings or broadcasts).
    private final long COOLDOWN_TIME = 50L;

    /**
     * Constructor initializes the listener.
     *
     * @param playerOpStorage Utility to check operator status.
     */
    public Chat(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for CHAT_MESSAGE packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle CHAT_MESSAGE packets (sent when a player types in chat).
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            handleChat(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for validating and processing chat messages.
     * This includes filtering offensive content and broadcasting clean messages.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleChat(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract the chat message.
        WrapperPlayClientChatMessage chatWrapper = new WrapperPlayClientChatMessage(event);
        String message = chatWrapper.getMessage();

        long currentTime = System.currentTimeMillis();
        long lastActionTime = lastActionTimes.getOrDefault(playerUUID, 0L);

        // Check if the message is valid (e.g., not offensive or malformed).
        if (!BadPacketsD.isValid(message)) {
            // Cancel the packet to prevent it from reaching other players.
            event.setCancelled(true);

            // Enforce cooldown before sending another warning.
            if (currentTime - lastActionTime >= COOLDOWN_TIME) {
                Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
                    player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Moderator] C.L.A.R.A: " +
                            ChatColor.RED + "Please refrain from using offensive terms.");
                });
                lastActionTimes.put(playerUUID, currentTime);
            }
            return;
        }

        // Format the clean message for broadcasting.
        String formattedMessage = "<" + player.getName() + "> " + message;

        // Cancel the original packet to prevent duplicate handling.
        event.setCancelled(true);

        // Enforce cooldown before broadcasting the message.
        if (currentTime - lastActionTime >= COOLDOWN_TIME) {
            Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
                Bukkit.broadcastMessage(formattedMessage);
            });
            lastActionTimes.put(playerUUID, currentTime);
        }
    }
}