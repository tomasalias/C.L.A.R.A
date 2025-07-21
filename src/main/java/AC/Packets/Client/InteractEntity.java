package AC.Packets.Client;

import AC.CLARA;
import AC.Checks.Combat.ReachCheckA;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.Optional;

public class InteractEntity extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(event.getPlayer(), event);
        }
    }

    private void handleInteract(Player player, PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity interactWrapper = new WrapperPlayClientInteractEntity(event);
        int entityID = interactWrapper.getEntityId();
        WrapperPlayClientInteractEntity.InteractAction action = interactWrapper.getAction();
        Optional<Vector3f> target = interactWrapper.getTarget();
        boolean sneaking = interactWrapper.isSneaking().orElse(false);

        // Fetch entities on the main thread
        Bukkit.getScheduler().runTask(CLARA.getInstance(), () -> {
            Entity entity = null;

            // Find the entity in the world using Bukkit API
            for (Entity e : player.getWorld().getEntities()) {
                if (e.getEntityId() == entityID) {
                    entity = e;
                    break;
                }
            }

            if (entity != null) {
                String entityType = entity.getType().name();
                System.out.println("[DEBUG] Found Entity Type: " + entityType);
                processInteract(player, action, target, entity, event);
            }
        });
    }

    private void processInteract(Player player, WrapperPlayClientInteractEntity.InteractAction action, Optional<Vector3f> target, Entity entity, PacketReceiveEvent event) {
        switch (action) {
            case INTERACT:
                System.out.println("[DEBUG] Player " + player.getName() + " interacted with entity: " + entity.getType().name());
                break;
            case ATTACK:
                if (entity instanceof Player victim) {
                    System.out.println("[DEBUG] Player " + player.getName() + " attacked another player: " + victim.getName());
                    ReachCheckA reachCheck = new ReachCheckA();
                    reachCheck.checkHit(player, victim);
                } else {
                    System.out.println("[DEBUG] Player " + player.getName() + " attacked a non-player entity: " + entity.getType().name());
                }
                break;
            case INTERACT_AT:
                if (target.isPresent()) {
                    Vector3f targetVec = target.get();
                    System.out.println("[DEBUG] Player " + player.getName() + " interacted at " + targetVec + " on entity ID: " + entity.getEntityId());
                } else {
                    System.out.println("[DEBUG] Missing target vector for INTERACT_AT action.");
                }
                break;
            default:
                System.out.println("[DEBUG] Unsupported interaction type (" + action + ")");
        }
        System.out.println("[DEBUG] Completed processing for player: " + player.getName());
    }
}