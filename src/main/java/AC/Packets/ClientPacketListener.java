package AC.Packets;

import AC.CLARA;
import AC.Packets.BadPackets.*;
import AC.Utils.CheckUtils.FastMath;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import AC.Utils.PluginUtils.sendPingPacket;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ClientPacketListener extends PacketListenerAbstract {


    // Reference to a utility that checks if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store whether a player is an operator, keyed by their UUID.
    // This avoids repeated expensive checks.
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Executor for running validation logic asynchronously to avoid blocking the main thread.
    private final ExecutorService executorService;

    // Stores the last time a player triggered a chat-related action.
    // Used to enforce cooldowns and prevent spam.
    private final Map<UUID, Long> lastActionTimes = new ConcurrentHashMap<>();

    // Cooldown time in milliseconds between chat actions (e.g., warnings or broadcasts).
    private final long COOLDOWN_TIME = 50L;

    // Tracks recent boat interactions to detect spam or exploit behavior.
    private final Map<UUID, Long> recentBoatClicks = new ConcurrentHashMap<>();

    // Maps to track rate-limiting and blacklisting of users by IP
    private static final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> blacklistMap = new ConcurrentHashMap<>();

    // Time window for rate-limiting in milliseconds (10 seconds)
    private static final int RATE_LIMIT_DURATION = 10000;

    // Duration for blacklisting in milliseconds (60 seconds)
    private static final int BLACKLIST_DURATION = 60000;



    public ClientPacketListener(ExecutorService executorService, PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGHEST);
        this.executorService = executorService;
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        User user = event.getUser();
        switch (event.getPacketType()) {
            case PacketType.Play.Client.PLAYER_ABILITIES -> handleAbilities(player, event);
            case PacketType.Play.Client.ANIMATION -> handleAnimation(player, event);
            case PacketType.Play.Client.PLAYER_DIGGING -> handleBlockDig(player, event);
            case PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> handleBlockPlace(player, event);
            case PacketType.Play.Client.CHAT_MESSAGE -> handleChat(player,event);
            case PacketType.Play.Client.HELD_ITEM_CHANGE -> handleHeldItemSlot(player, event);
            case PacketType.Play.Client.INTERACT_ENTITY -> handleInteractEntity(player, event);
            case PacketType.Login.Client.LOGIN_START -> handleLoginStart(user, event);
            case PacketType.Play.Client.PLAYER_ROTATION -> handleLook(player, event);
            case PacketType.Play.Client.PONG -> handlePong(player, event);
            case PacketType.Play.Client.PLAYER_POSITION -> handlePosition(player, event);
            case PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION -> handlePositionLook(player, event);
            case PacketType.Play.Client.STEER_VEHICLE -> handleSteerVehicle(player, event);
            case PacketType.Play.Client.VEHICLE_MOVE -> handleVehicleMove(player, event);
            default -> handleUnhandledPacket(player, event);
        }
    }

    private void handleUnhandledPacket(Player player, PacketReceiveEvent event) {
        // Log or handle unexpected packet types
    }

    private void handleAbilities(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // We cache this result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data (e.g., isFlying).
        WrapperPlayClientPlayerAbilities abilitiesWrapper = new WrapperPlayClientPlayerAbilities(event);
        boolean isFlying = abilitiesWrapper.isFlying();

        // Run validation asynchronously to avoid lagging the server's main thread.
        executorService.execute(() -> {
            // Validate flying state using custom logic (likely anti-cheat).
            if (!BadPacketsE.isValidFlying(player, isFlying)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "E");
            }
        });
    }

    private void handleAnimation(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientAnimation swingWrapper = new WrapperPlayClientAnimation(event);

        // Extract the hand used in the swing (MAIN_HAND or OFF_HAND).
        int handOrdinal = swingWrapper.getHand().ordinal();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the hand value using custom anti-cheat logic.
            // Valid ordinals are typically 0 (MAIN_HAND) and 1 (OFF_HAND).
            if (!BadPacketsL.isValid(handOrdinal)) {
                KickMessages.kickPlayerForInvalidPacket(player, "F");
            }
        });
    }

    private void handleBlockDig(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientPlayerDigging blockDiggingWrapper = new WrapperPlayClientPlayerDigging(event);

        // Extract the type of digging action (e.g., START_DESTROY_BLOCK, CANCEL_DESTROY_BLOCK).
        DiggingAction action = blockDiggingWrapper.getAction();

        // Extract the block position being interacted with.
        Vector3i blockPosition = blockDiggingWrapper.getBlockPosition();

        // Extract the face of the block being targeted (e.g., NORTH, UP).
        BlockFace blockFace = blockDiggingWrapper.getBlockFace();

        // Extract the sequence number (used for tracking packet order).
        int sequence = blockDiggingWrapper.getSequence();

        // Break down the block position into individual coordinates.
        int locationX = blockPosition.x;
        int locationY = blockPosition.y;
        int locationZ = blockPosition.z;

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Convert the action to its ordinal value (used for internal validation).
            int actionOrdinal = action != null ? action.ordinal() : -1;

            // Validate the digging action using custom anti-cheat logic.
            if (!BadPacketsJ.isValid(player, locationX, locationY, locationZ, blockFace, action, actionOrdinal)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "J");
            }
        });
    }

    private void handleBlockPlace(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientPlayerBlockPlacement blockPlacementWrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        // Extract the face ID (numeric representation of the block face being targeted).
        int faceId = blockPlacementWrapper.getFaceId();

        // Extract the block face (e.g., NORTH, UP, DOWN).
        BlockFace face = blockPlacementWrapper.getFace();

        // Extract the cursor position (where the player is aiming on the block surface).
        Vector3f cursorPosition = blockPlacementWrapper.getCursorPosition();

        // Check if the player is placing the block inside another block (can be used for exploit detection).
        boolean insideBlock = blockPlacementWrapper.getInsideBlock().orElse(false);

        // Extract the sequence number (used for tracking packet order).
        int sequence = blockPlacementWrapper.getSequence();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the block placement using custom anti-cheat logic.
            if (!BadPacketsI.isValid(player, faceId, cursorPosition.x, cursorPosition.y, cursorPosition.z, insideBlock, sequence)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "I");
            }
        });
    }

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

    private void handleHeldItemSlot(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientHeldItemChange heldItemWrapper = new WrapperPlayClientHeldItemChange(event);

        // Extract the hotbar slot index the player is switching to.
        int slot = heldItemWrapper.getSlot();

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the slot index using custom anti-cheat logic.
            // Typically, valid slots are 0–8 (standard hotbar range).
            if (!BadPacketsF.isValid(slot)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "F");
            }
        });
    }

    private void handleInteractEntity(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured data.
        WrapperPlayClientInteractEntity interactWrapper = new WrapperPlayClientInteractEntity(event);

        // Extract the entity ID being interacted with.
        int entityID = interactWrapper.getEntityId();

        // Extract the type of interaction (e.g., ATTACK, INTERACT).
        WrapperPlayClientInteractEntity.InteractAction action = interactWrapper.getAction();

        // Extract the target vector (used for INTERACT_AT).
        Optional<Vector3f> target = interactWrapper.getTarget();

        // Check if the player was sneaking during the interaction.
        boolean sneaking = interactWrapper.isSneaking().orElse(false);

        // Schedule entity lookup and interaction processing on the main thread.
        Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
            Entity entity = null;

            // Search for the entity in the player's world by matching entity ID.
            for (Entity e : player.getWorld().getEntities()) {
                if (e.getEntityId() == entityID) {
                    entity = e;
                    break;
                }
            }

            // If the entity was found, process the interaction.
            if (entity != null) {
                System.out.println("[DEBUG] Found Entity Type: " + entity.getType().name());
                processInteract(player, action, target, entity, event);
            }
        });
    }

    /**
     * Processes the interaction based on its type and the entity involved.
     *
     * @param player  The player performing the interaction.
     * @param action  The type of interaction.
     * @param target  Optional target vector (for INTERACT_AT).
     * @param entity  The entity being interacted with.
     * @param event   The original packet event.
     */
    private void processInteract(Player player, WrapperPlayClientInteractEntity.InteractAction action,
                                 Optional<Vector3f> target, Entity entity, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();

        switch (action) {
            case INTERACT -> {
                // Handle right-click interactions.
                if (entity instanceof Boat) {
                    // Flag recent boat clicks for potential exploit detection.
                    flagBoatClick(playerUUID);
                }
            }
            case ATTACK -> {
                // Handle left-click (attack) interactions.
                if (entity instanceof Player victim) {
                    System.out.println("[DEBUG] Player " + player.getName() + " attacked another player: " + victim.getName());
                    // Future use: trigger combat checks or velocity analysis.
                }
            }
            case INTERACT_AT -> {
                // Handle precise targeting interactions.
                if (target.isPresent()) {
                    Vector3f targetVec = target.get();
                    // Future use: analyze suspicious targeting or reach exploits.
                } else {
                    System.out.println("[DEBUG] Missing target vector for INTERACT_AT action.");
                }
            }
            default -> System.out.println("[DEBUG] Unsupported interaction type (" + action + ")");
        }
    }

    /**
     * Flags a player as having recently clicked a boat.
     * Used for timing-based exploit detection.
     *
     * @param playerUUID The UUID of the player.
     */
    public void flagBoatClick(UUID playerUUID) {
        recentBoatClicks.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * Checks if a player has clicked a boat within the last 2 seconds.
     * Useful for detecting rapid or automated interactions.
     *
     * @param playerUUID The UUID of the player.
     * @return True if the player clicked a boat recently.
     */
    public boolean didRecentlyClickBoat(UUID playerUUID) {
        Long timestamp = recentBoatClicks.get(playerUUID);
        return timestamp != null && System.currentTimeMillis() - timestamp <= 2000;
    }

    private void handleLoginStart(User user, PacketReceiveEvent event) {
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

    private void handleLook(Player player, PacketReceiveEvent event) {
        long ts = System.currentTimeMillis();

        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator (admin). Operators are trusted and bypass checks.
        // Cache the result to avoid repeated lookups.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip validation for operators.
        }

        // Wrap the raw packet to extract structured rotation data.
        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);

        // Normalize yaw to ensure it's within expected bounds (e.g., -180 to 180 degrees).
        final float normalizedYaw = FastMath.normalizeAngle(wrapper.getYaw());

        // Extract pitch (vertical look angle).
        final float pitch = wrapper.getPitch();

        // Retrieve player data for packet comparison and tracking.
        PlayerData data = CLARA.getPlayerData(playerUUID);

        // Check if this packet is identical to the last one.
        if (data.isSameAsLastRotation(wrapper)) {
            return; // Skip processing if it's a duplicate.
        }

        // Store the current packet as the new reference.
        data.setLastRotationPacket(wrapper);

        // Update the wrapper with the normalized yaw value.
        wrapper.setYaw(normalizedYaw);

        // Run validation asynchronously to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the rotation using custom anti-cheat logic.
            if (!BadPacketsC.isValid(normalizedYaw, pitch)) {
                // If invalid, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "C");
            }
            CLARA.getInstance()
                    .getTimer()
                    .recordPacket(
                            player,
                            player.getUniqueId(),
                            ts,
                            CLARA.getPlayerData(player.getUniqueId()),
                            PacketKind.LOOK
                    );

        });
    }

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

    private void handlePosition(Player player, PacketReceiveEvent event) {
        long ts = System.currentTimeMillis();
        // Retrieve the player's unique identifier for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. We cache this result to avoid repeated permission checks.
        // If the cache doesn't contain the UUID, we query the PlayerOpStorage and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing for them.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Attempt to wrap the raw packet into a structured format to extract movement data.
        // If the packet is malformed or wrapping fails, we log the error and skip processing.
        WrapperPlayClientPlayerPosition wrapper;
        try {
            wrapper = new WrapperPlayClientPlayerPosition(event);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Extract the movement coordinates (x, y, z) and the onGround flag from the packet.
        final double x = wrapper.getPosition().getX();
        final double y = wrapper.getPosition().getY();
        final double z = wrapper.getPosition().getZ();
        final boolean onGround = wrapper.isOnGround();

        // Retrieve player data for packet comparison.
        PlayerData data = CLARA.getPlayerData(playerUUID);

        // Check if this packet is identical to the last one.
        if (data.isSameAsLastPosition(wrapper)) {
            return; // Skip processing if it's a duplicate.
        }

        // Store the current packet as the new reference.
        data.setLastPositionPacket(wrapper);


        CLARA.getInstance()
                .getTimer()
                .recordPacket(
                        player,
                        player.getUniqueId(),
                        ts,
                        CLARA.getPlayerData(player.getUniqueId()),
                        PacketKind.POSITION
                );
    }

    private void handlePositionLook(Player player, PacketReceiveEvent event) {
        long ts = System.currentTimeMillis();
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Attempt to wrap the raw packet into a structured format.
        WrapperPlayClientPlayerPositionAndRotation wrapper;
        try {
            wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Normalize yaw and update the wrapper.
        final float normalizedYaw = FastMath.normalizeAngle(wrapper.getYaw());
        wrapper.setYaw(normalizedYaw);

        // Extract other packet data.
        final double x = wrapper.getPosition().getX();
        final double y = wrapper.getPosition().getY();
        final double z = wrapper.getPosition().getZ();
        final float pitch = wrapper.getPitch();
        final boolean onGround = wrapper.isOnGround();

        // Retrieve player data for packet comparison.
        PlayerData data = CLARA.getPlayerData(playerUUID);

        // Check if this packet is identical to the last one.
        if (data.isSameAsLastPositionLook(wrapper)) {
            return; // Skip processing if it's a duplicate.
        }

        // Store the current packet as the new reference.
        data.setLastPositionLookPacket(wrapper);

        // Offload validation and recording to a background thread.
        executorService.execute(() -> {
            try {
                if (!BadPacketsA.isValid(player, x, y, z, normalizedYaw, pitch)) {
                    KickMessages.kickPlayerForInvalidPacket(player, "A");
                    return;
                }

                CLARA.getInstance()
                        .getTimer()
                        .recordPacket(
                                player,
                                playerUUID,
                                ts,
                                data,
                                PacketKind.POSITION_AND_ROTATION
                        );

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleSteerVehicle(Player player, PacketReceiveEvent event) {
        // Retrieve the player's UUID for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Wrap the raw packet to extract structured steering input.
        WrapperPlayClientSteerVehicle steerVehicleWrapper = new WrapperPlayClientSteerVehicle(event);

        // Extract directional input values and action flags.
        final float forward = steerVehicleWrapper.getForward();
        final float sideways = steerVehicleWrapper.getSideways();
        final boolean jump = steerVehicleWrapper.isJump();
        final boolean unmount = steerVehicleWrapper.isUnmount();

        // Offload validation logic to a background thread to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the steering input using anti-cheat logic.
            // This typically checks for impossible or manipulated values.
            if (!BadPacketsH.isValidSteerMovement(forward, sideways)) {
                // If validation fails, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "H");
            }
        });
    }

    private void handleVehicleMove(Player player, PacketReceiveEvent event) {
        // Retrieve the player's UUID for tracking and caching.
        UUID playerUUID = player.getUniqueId();

        // Check if the player is an operator. If not cached, query and store the result.
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));

        // Operators are typically exempt from anti-cheat checks, so we skip further processing.
        if (Boolean.TRUE.equals(isOp)) {
            return;
        }

        // Wrap the raw packet to extract structured vehicle movement data.
        WrapperPlayClientVehicleMove vehicleMoveWrapper = new WrapperPlayClientVehicleMove(event);

        // Extract movement coordinates and orientation angles from the packet.
        final double x = vehicleMoveWrapper.getPosition().getX();
        final double y = vehicleMoveWrapper.getPosition().getY();
        final double z = vehicleMoveWrapper.getPosition().getZ();
        final float yaw = vehicleMoveWrapper.getYaw();
        final float pitch = vehicleMoveWrapper.getPitch();
        // Normalize yaw to ensure it's within expected bounds (e.g., -180 to 180 degrees).
        final float normalizedYaw = FastMath.normalizeAngle(vehicleMoveWrapper.getYaw());
        // Update the wrapper with the normalized yaw value.
        vehicleMoveWrapper.setYaw(normalizedYaw);


        System.out.println("[VEHICLE_MOVE DEBUG] Player: " + player.getName());
        System.out.println("  Position: X=" + x + ", Y=" + y + ", Z=" + z);
        System.out.println("  Rotation: Yaw=" + normalizedYaw + ", Pitch=" + pitch);


        // Offload validation logic to a background thread to avoid blocking the main server thread.
        executorService.execute(() -> {
            // Validate the vehicle movement using anti-cheat logic.
            // This typically checks for impossible or manipulated values.
            if (!BadPacketsG.isValid(x, y, z, normalizedYaw, pitch)) {
                // If validation fails, kick the player with a predefined message.
                KickMessages.kickPlayerForInvalidPacket(player, "G");
            }
        });
    }
}