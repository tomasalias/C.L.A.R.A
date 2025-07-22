package AC.Packets.Client;

import AC.CLARA;
import AC.Checks.Combat.ReachCheckA;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractEntity extends PacketListenerAbstract {

    private final PlayerOpStorage playerOpStorage;
    private final Map<UUID, Boolean> opCache = new ConcurrentHashMap<>();

    public InteractEntity(PlayerOpStorage playerOpStorage) {
        this.playerOpStorage = playerOpStorage;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(event.getPlayer(), event);
        }
    }

    private void handleInteract(Player player, PacketReceiveEvent event) {
        UUID playerUUID = player.getUniqueId();
        Boolean isOp = opCache.computeIfAbsent(playerUUID, id -> playerOpStorage.isPlayerOperator(player));
        if (Boolean.TRUE.equals(isOp)) {
            return; // Skip enforcement for operators
        }

        WrapperPlayClientInteractEntity interactWrapper = new WrapperPlayClientInteractEntity(event);
        int entityID = interactWrapper.getEntityId();
        WrapperPlayClientInteractEntity.InteractAction action = interactWrapper.getAction();
        Optional<Vector3f> target = interactWrapper.getTarget();
        boolean sneaking = interactWrapper.isSneaking().orElse(false);

        Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
            Entity entity = null;
            for (Entity e : player.getWorld().getEntities()) {
                if (e.getEntityId() == entityID) {
                    entity = e;
                    break;
                }
            }

            if (entity != null) {
                System.out.println("[DEBUG] Found Entity Type: " + entity.getType().name());
                processInteract(player, action, target, entity, event);
            }
        });
    }

    private void processInteract(Player player, WrapperPlayClientInteractEntity.InteractAction action,
                                 Optional<Vector3f> target, Entity entity, PacketReceiveEvent event) {
        switch (action) {
            case INTERACT -> System.out.println("[DEBUG] Player " + player.getName() + " interacted with entity: " + entity.getType().name());
            case ATTACK -> {
                if (entity instanceof Player victim) {
                    System.out.println("[DEBUG] Player " + player.getName() + " attacked another player: " + victim.getName());
                    ReachCheckA reachCheck = new ReachCheckA();
                    reachCheck.checkHit(player, victim);
                } else {
                    System.out.println("[DEBUG] Player " + player.getName() + " attacked a non-player entity: " + entity.getType().name());
                }
            }
            case INTERACT_AT -> {
                if (target.isPresent()) {
                    Vector3f targetVec = target.get();
                    System.out.println("[DEBUG] Player " + player.getName() + " interacted at " + targetVec +
                            " on entity ID: " + entity.getEntityId());
                } else {
                    System.out.println("[DEBUG] Missing target vector for INTERACT_AT action.");
                }
            }
            default -> System.out.println("[DEBUG] Unsupported interaction type (" + action + ")");
        }
        System.out.println("[DEBUG] Completed processing for player: " + player.getName());
    }
}