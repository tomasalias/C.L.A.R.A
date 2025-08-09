package AC.Packets.Client;

import AC.CLARA;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.sendPingPacket;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * This class listens for PONG packets sent by clients.
 * These packets are responses to server-sent PING packets and are used to measure latency.
 * We use this to calculate player ping and update their PlayerData.
 */
public class Pong extends PacketListenerAbstract {

    /**
     * Constructor sets the listener priority to HIGHEST for early interception.
     * This ensures we process the packet before other systems.
     */
    public Pong() {
        super(PacketListenerPriority.HIGHEST);
    }

    /**
     * Called when a packet is received from a client.
     * Filters for PONG packets and processes them.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle PONG packets (sent in response to server PING).
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            handlePong(event.getPlayer(), event);
        }
    }

    /**
     * Handles the PONG packet.
     * Calculates the player's ping based on the timestamp of the original PING packet.
     * Updates the player's ping data and triggers another ping for continuous monitoring.
     *
     * @param player The player who responded with a PONG packet.
     * @param event  The packet event containing the data.
     */
    private void handlePong(Player player, PacketReceiveEvent event) {
        // Get the current server time.
        long currentTimestamp = System.currentTimeMillis();

        // Get the player's name (used as key for ping tracking).
        String playerName = player.getName();

        // Retrieve the timestamp when the server sent the PING packet.
        Long sentTimestamp = sendPingPacket.getPingTimestamp(playerName);

        // If we have a recorded timestamp, calculate ping.
        if (sentTimestamp != null) {
            // Calculate ping as the round-trip time between PING and PONG.
            long playerPing = currentTimestamp - sentTimestamp;

            // Retrieve the player's UUID and associated PlayerData object.
            UUID playerUUID = player.getUniqueId();
            PlayerData playerData = CLARA.getPlayerData(playerUUID);

            // If PlayerData exists, update ping and timestamp.
            if (playerData != null) {
                playerData.setPing(playerPing);
                playerData.setPingTimestamp(currentTimestamp);
            }

            // Send another PING packet to continue monitoring latency.
            sendPingPacket.triggerPing(player);
        }
    }
}