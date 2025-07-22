package AC.Checks.Combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReachCheckB {

    // Dynamically collect all full-cube, occluding blocks at class load
    private static final Set<Material> VALID_BLOCKS =
            Arrays.stream(Material.values())
                    .filter(Material::isOccluding)
                    .collect(Collectors.toUnmodifiableSet());

    private static final double MAX_BLOCK_REACH_DISTANCE = 4.5;
    private static final Logger logger = Logger.getLogger("ReachCheckB");

    public static void perform(Player player, Vector sampledEyePos, int blockX, int blockY, int blockZ) {
        String playerName = player.getName();
        logger.info("[ReachCheckB] === Block Reach Check Start ===");
        logger.info("[ReachCheckB] Player: " + playerName);
        logger.info("[ReachCheckB] Sampled Eye Position: " + sampledEyePos);

        Material blockType = player.getWorld()
                .getBlockAt(blockX, blockY, blockZ)
                .getType();
        logger.info("[ReachCheckB] Block Type: " + blockType +
                " at [" + blockX + ", " + blockY + ", " + blockZ + "]");

        if (!VALID_BLOCKS.contains(blockType)) {
            logger.info("[ReachCheckB] Block type is exempt from reach checks. Skipping.");
            return;
        }

        Vector direction = player.getEyeLocation()
                .getDirection()
                .normalize();
        Vector reachEnd = sampledEyePos.clone()
                .add(direction.multiply(MAX_BLOCK_REACH_DISTANCE));

        logger.info("[ReachCheckB] Ray Direction: " + direction);
        logger.info("[ReachCheckB] Ray Endpoint: " + reachEnd);

        double distance = getClosestHitDistance(sampledEyePos, reachEnd, blockX, blockY, blockZ);

        if (distance == -1) {
            logger.warning("[ReachCheckB] No valid intersection with block hitbox.");
            return;
        }

        String formattedDistance = String.format("%.4f", distance);
        logger.info("[ReachCheckB] Block intersection distance: " + formattedDistance + " blocks");

        if (distance > MAX_BLOCK_REACH_DISTANCE) {
            logger.warning("[ReachCheckB] Reach violation! Player: " + playerName +
                    " broke block from: " + formattedDistance + " blocks");
            player.sendMessage("⚠️ You interacted with a block from too far: " + formattedDistance + " blocks.");
            // Kick or flag here
        } else {
            logger.info("[ReachCheckB] Valid block break at distance: " + formattedDistance + " blocks.");
        }
    }

    private static double getClosestHitDistance(Vector rayOrigin, Vector rayEnd,
                                                int blockX, int blockY, int blockZ) {
        double minX = blockX, minY = blockY, minZ = blockZ;
        double maxX = blockX + 1, maxY = blockY + 1, maxZ = blockZ + 1;

        Vector dir = rayEnd.clone().subtract(rayOrigin);
        Vector invDir = new Vector(
                1.0 / (dir.getX() == 0 ? Double.MIN_VALUE : dir.getX()),
                1.0 / (dir.getY() == 0 ? Double.MIN_VALUE : dir.getY()),
                1.0 / (dir.getZ() == 0 ? Double.MIN_VALUE : dir.getZ())
        );

        double t1 = (minX - rayOrigin.getX()) * invDir.getX();
        double t2 = (maxX - rayOrigin.getX()) * invDir.getX();
        double t3 = (minY - rayOrigin.getY()) * invDir.getY();
        double t4 = (maxY - rayOrigin.getY()) * invDir.getY();
        double t5 = (minZ - rayOrigin.getZ()) * invDir.getZ();
        double t6 = (maxZ - rayOrigin.getZ()) * invDir.getZ();

        double tMin = Math.max(
                Math.max(Math.min(t1, t2), Math.min(t3, t4)),
                Math.min(t5, t6)
        );
        double tMax = Math.min(
                Math.min(Math.max(t1, t2), Math.max(t3, t4)),
                Math.max(t5, t6)
        );

        logger.info("[ReachCheckB] AABB tMin: " + tMin + ", tMax: " + tMax);

        if (tMax < tMin || tMax < 0) {
            return -1; // No valid intersection
        }

        Vector intersectionPoint = rayOrigin.clone().add(dir.multiply(tMin));
        double distance = rayOrigin.distance(intersectionPoint);

        logger.info("[ReachCheckB] Hit at point: " + intersectionPoint);
        logger.info("[ReachCheckB] Calculated reach distance: " +
                String.format("%.4f", distance));

        return distance;
    }
}