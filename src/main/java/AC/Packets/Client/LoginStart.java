package AC.Packets.Client;

import AC.Packets.BadPackets.BadPacketsK;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class listens for LOGIN_START packets sent by clients.
 * These packets are triggered when a player initiates a login.
 * We validate login data, apply rate-limiting, and blacklist suspicious IPs.
 */
public class LoginStart extends PacketListenerAbstract {

    // Maps to track rate-limiting and blacklisting of users by IP
    private static final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> blacklistMap = new ConcurrentHashMap<>();

    // Time window for rate-limiting in milliseconds (10 seconds)
    private static final int RATE_LIMIT_DURATION = 10000;

    // Duration for blacklisting in milliseconds (60 seconds)
    private static final int BLACKLIST_DURATION = 60000;

    /**
     * Constructor sets the listener priority to HIGHEST for early interception.
     */
    public LoginStart() {
        super(PacketListenerPriority.HIGHEST);
    }

    /**
     * Called when a packet is received from a client.
     * Filters for LOGIN_START packets and processes them.
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();

        // Only handle LOGIN_START packets
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            handleLoginStart(event, user);
        }
    }

    /**
     * Handles the LOGIN_START packet.
     * Applies rate-limiting and blacklisting based on IP, and validates login data.
     * @param event The packet event.
     * @param user The user attempting to log in.
     */
    private void handleLoginStart(PacketReceiveEvent event, User user) {
        // Get the user's IP address
        String userIP = user.getAddress().getAddress().getHostAddress();

        // Extract the network portion of the IP (first three segments)
        String playerIP = getNetworkPortion(userIP);

        // Step 1: Check if the IP is blacklisted
        if (isBlacklisted(playerIP)) {
            event.setCancelled(true);
            user.sendMessage("You have been blacklisted due to suspicious activity. Please wait and try again later.");
            return;
        }

        // Step 2: Check if the IP is rate-limited
        if (isRateLimited(playerIP)) {
            event.setCancelled(true);
            user.sendMessage("You have been rate-limited. Please try again later.");
            return;
        }

        // Step 3: Log the login attempt for rate-limiting
        rateLimitMap.put(playerIP, System.currentTimeMillis());

        // Step 4: Extract and validate login data
        WrapperLoginClientLoginStart loginWrapper = new WrapperLoginClientLoginStart(event);
        String username = loginWrapper.getUsername();
        Optional<UUID> playerUUID = loginWrapper.getPlayerUUID();
        String uuidString = playerUUID.map(UUID::toString).orElse(null);

        // Validate username and UUID using anti-cheat logic
        boolean isValid = BadPacketsK.isValid(username, uuidString);

        // Step 5: Blacklist IP if login data is invalid
        if (!isValid) {
            blacklistMap.put(playerIP, System.currentTimeMillis() + BLACKLIST_DURATION);
            event.setCancelled(true);
        }
    }

    /**
     * Extracts the first three segments of an IPv4 address.
     * Used to group similar IPs for rate-limiting and blacklisting.
     * @param ip Full IP address.
     * @return Network portion of the IP.
     */
    private String getNetworkPortion(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return ip;
    }

    /**
     * Checks if the IP is currently rate-limited.
     * @param playerIP Network portion of the IP.
     * @return True if rate-limited, false otherwise.
     */
    private boolean isRateLimited(String playerIP) {
        Long lastRequestTime = rateLimitMap.get(playerIP);
        if (lastRequestTime == null) return false;
        long elapsedTime = System.currentTimeMillis() - lastRequestTime;
        return elapsedTime < RATE_LIMIT_DURATION;
    }

    /**
     * Checks if the IP is currently blacklisted.
     * @param playerIP Network portion of the IP.
     * @return True if blacklisted, false otherwise.
     */
    private boolean isBlacklisted(String playerIP) {
        Long blacklistExpiry = blacklistMap.get(playerIP);
        if (blacklistExpiry == null) return false;
        return System.currentTimeMillis() < blacklistExpiry;
    }
}