package AC.Checks.Combat;

import AC.CLARA;
import AC.Utils.CheckUtils.Hitbox;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.CheckUtils.Position;
import AC.Utils.PluginUtils.KickMessages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Getter
public class ReachCheckA {
    private final Map<UUID, Hitbox> hitboxes = new HashMap<>();
    private static final double MAX_REACH_DISTANCE = 4.0;
    private static final double KICK_REACH_DISTANCE = 3.0;
    private static final Logger logger = Logger.getLogger("ReachCheckA");

    public void updateHitbox(Player player) {
        System.out.println("[ReachCheckA] Updating hitbox for player: " + player.getName());
        Hitbox playerHitbox = hitboxes.computeIfAbsent(player.getUniqueId(), key -> new Hitbox());
        playerHitbox.setSneaking(player.isSneaking());

        Vector3D convertedPosition = new Vector3D(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()
        );
        playerHitbox.updatePosition(convertedPosition);

        System.out.println("[ReachCheckA] Hitbox updated for player: " + player.getName() + ", Sneaking: " + player.isSneaking());
    }

    public void checkHit(Entity attacker, Entity victim) {
        System.out.println("[ReachCheckA] Checking hit between attacker: " + attacker.getName() + " and victim: " + victim.getName());

        if (!(attacker instanceof Player playerAttacker)) {
            System.out.println("[ReachCheckA] Attacker is not a player, skipping reach check.");
            return;
        }

        if (!(victim instanceof Player playerVictim)) {
            System.out.println("[ReachCheckA] Victim is not a player, skipping reach check.");
            return;
        }

        updateHitbox(playerAttacker);
        updateHitbox(playerVictim);

        long attackerPing = (long) CLARA.getPlayerData(playerAttacker.getUniqueId()).getCurrentPing();
        System.out.println("[ReachCheckA] Attacker Ping: " + attackerPing);

        Vector attackerPosition = getPlayerPositionAtTime(playerAttacker.getUniqueId(), attackerPing);
        Vector victimPosition = getPlayerPositionAtTime(playerVictim.getUniqueId(), attackerPing);

        if (attackerPosition == null || victimPosition == null) {
            System.out.println("[ReachCheckA] Invalid position(s) retrieved. Skipping reach check.");
            return;
        }

        System.out.println("[ReachCheckA] Victim position: " + victimPosition);
        System.out.println("[ReachCheckA] Attacker position: " + attackerPosition);

        attackerPosition.setY(attackerPosition.getY() + 1.62);

        Vector direction = playerAttacker.getEyeLocation().getDirection().normalize();
        Vector reachEndPoint = attackerPosition.clone().add(direction.multiply(MAX_REACH_DISTANCE));

        Hitbox victimHitbox = hitboxes.get(playerVictim.getUniqueId());
        double intersectionDistance = victimHitbox.checkLineIntersection(attackerPosition, reachEndPoint);

        if (intersectionDistance == -1) {
            System.out.println("[ReachCheckA] Hit too far or no intersection.");
            logger.info("[ReachCheckA] Kicking player " + playerAttacker.getName() + " for failed intersection.");
            KickMessages.kickPlayerForReachCheck(playerAttacker, "No valid intersection");
            return;
        }

        String formattedDistance = String.format("%.2f", intersectionDistance);

        if (intersectionDistance > KICK_REACH_DISTANCE) {
            System.out.println("[ReachCheckA] Reach violation detected: " + formattedDistance + " blocks.");
            logger.info("[ReachCheckA] Kicking player " + playerAttacker.getName() + " for reach violation: " + formattedDistance + " blocks.");
            KickMessages.kickPlayerForReachCheck(playerAttacker, formattedDistance);
            return;
        }

        playerAttacker.sendMessage("You hit the target at: " + formattedDistance + " blocks.");
        playerVictim.sendMessage("You were hit at: " + formattedDistance + " blocks.");
    }

    public Vector getPlayerPositionAtTime(UUID entityUUID, long timeAgo) {
        System.out.println("[ReachCheckA] Retrieving position for Player UUID: " + entityUUID + " at time: " + timeAgo + "ms ago");
        PlayerData playerData = CLARA.getPlayerData(entityUUID);
        Position position = playerData.getPositionAtTime(timeAgo);

        if (position == null) {
            System.out.println("[ReachCheckA] No position found for Player UUID: " + entityUUID + " at time: " + timeAgo);
            return null;
        }

        System.out.println("[ReachCheckA] Position for Player UUID: " + entityUUID + " at time " + timeAgo + ": " + position);
        return new Vector(position.getX(), position.getY(), position.getZ());
    }
}