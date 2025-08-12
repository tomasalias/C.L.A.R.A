package AC.Packets.Client;

import AC.CLARA;
import AC.Checks.Movement.VelocityCheckA;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Boat;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class listens for INTERACT_ENTITY packets sent by clients.
 * These packets are triggered when a player interacts with an entity (e.g., right-click, attack).
 * We use this to detect suspicious interactions, such as rapid boat clicks or player attacks.
 */
public class InteractEntity extends PacketListenerAbstract {

    // Utility to check if a player is an operator (admin privileges).
    private final PlayerOpStorage playerOpStorage;

    // Cache to store operator status for each player, keyed by UUID.
    // Prevents repeated permission checks.
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    // Tracks recent boat interactions to detect spam or exploit behavior.
    private final Map<UUID, Long> recentBoatClicks = new ConcurrentHashMap<>();

    /**
     * Constructor sets up the listener with highest priority.
     * This ensures our logic runs before other plugins or systems.
     *
     * @param playerOpStorage Utility to check operator status.
     */
    public InteractEntity(PlayerOpStorage playerOpStorage) {
        super(PacketListenerPriority.HIGH);
        this.playerOpStorage = playerOpStorage;
    }

    /**
     * Called when a packet is received from a client.
     * Filters for INTERACT_ENTITY packets only.
     *
     * @param event The packet receive event.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle INTERACT_ENTITY packets (sent when a player interacts with an entity).
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(event.getPlayer(), event);
        }
    }

    /**
     * Handles the logic for processing entity interactions.
     * This includes identifying the entity and delegating to specific handlers.
     *
     * @param player The Bukkit player who sent the packet.
     * @param event  The packet event containing raw data.
     */
    private void handleInteract(Player player, PacketReceiveEvent event) {
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
}