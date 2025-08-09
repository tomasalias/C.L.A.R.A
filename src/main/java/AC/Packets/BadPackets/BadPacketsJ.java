package AC.Packets.BadPackets;

import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Utility class for validating block digging and item interaction packets.
 * Ensures that coordinates, actions, and block faces are within valid bounds,
 * and that digging actions occur within a reasonable radius from the player.
 */
@UtilityClass
public class BadPacketsJ {

    // Valid status values for digging/item interaction packets
    private static final int MIN_STATUS_VALUE = 0;
    private static final int MAX_STATUS_VALUE = 6;

    // Valid block face values (0 = DOWN, 1 = UP, etc.)
    private static final int MIN_FACE_VALUE = 0;
    private static final int MAX_FACE_VALUE = 5;

    // Valid coordinate bounds (based on Minecraft world limits)
    private static final int MIN_VALID_POSITION = -30_000_000;
    private static final int MAX_VALID_POSITION = 30_000_000;

    // Maximum allowed radius for digging actions (squared distance check)
    private static final double MAX_DIG_RADIUS = 6; // Slightly above 5.74456 for tolerance

    /**
     * Validates a digging or item interaction packet.
     *
     * @param player     The player performing the action
     * @param locationX  X-coordinate of the block being interacted with
     * @param locationY  Y-coordinate of the block
     * @param locationZ  Z-coordinate of the block
     * @param blockFace  The face of the block being targeted
     * @param action     The specific digging or item action
     * @param status     The status code representing the type of interaction
     * @return true if the packet is valid and consistent; false otherwise
     */
    public boolean isValid(Player player, int locationX, int locationY, int locationZ, BlockFace blockFace, DiggingAction action, int status) {
        // Check if status is within the valid range
        if (status < MIN_STATUS_VALUE || status > MAX_STATUS_VALUE) {
            return false;
        }

        // Handle different status types
        switch (status) {
            case 0: // Start digging
            case 1: // Cancel digging
            case 2: // Finish digging
                // Validate coordinates and block face
                if (!isWithinCoordinateBounds(locationX, locationY, locationZ) || !isWithinFaceBounds(blockFace)) {
                    return false;
                }

                // If the action is not exempt from radius checks, validate proximity
                if (!isActionExemptFromRadiusCheck(action)) {
                    return isWithinDiggingRadius(player, locationX, locationY, locationZ);
                }

                // Valid digging action
                return true;

            case 3: // Drop item stack
            case 4: // Drop item
            case 5: // Shoot arrow / finish eating
            case 6: // Swap item in hand
                // These actions are exempt from coordinate checks but must match a specific pattern
                return isValidExemptAction(locationX, locationY, locationZ, blockFace);

            default:
                // Unknown status
                return false;
        }
    }

    /**
     * Checks if the action type is exempt from radius validation.
     * These actions are not spatially bound to block coordinates.
     */
    private boolean isActionExemptFromRadiusCheck(DiggingAction action) {
        return action == DiggingAction.RELEASE_USE_ITEM
                || action == DiggingAction.DROP_ITEM
                || action == DiggingAction.DROP_ITEM_STACK
                || action == DiggingAction.SWAP_ITEM_WITH_OFFHAND;
    }

    /**
     * Validates exempt actions by checking for a specific coordinate and face pattern.
     * These actions should have zeroed coordinates and target the DOWN face.
     */
    private boolean isValidExemptAction(int x, int y, int z, BlockFace blockFace) {
        return x == 0 && y == 0 && z == 0 && blockFace == BlockFace.DOWN;
    }

    /**
     * Checks if the block face value is within the valid range.
     */
    private boolean isWithinFaceBounds(BlockFace blockFace) {
        int value = blockFace.getFaceValue();
        return value >= MIN_FACE_VALUE && value <= MAX_FACE_VALUE;
    }

    /**
     * Checks if the block coordinates are within the valid world bounds.
     */
    private boolean isWithinCoordinateBounds(int x, int y, int z) {
        return x >= MIN_VALID_POSITION && x <= MAX_VALID_POSITION
                && y >= MIN_VALID_POSITION && y <= MAX_VALID_POSITION
                && z >= MIN_VALID_POSITION && z <= MAX_VALID_POSITION;
    }

    /**
     * Checks if the block being interacted with is within a reasonable radius of the player's eye location.
     * Uses squared distance to avoid unnecessary square root calculations.
     */
    private boolean isWithinDiggingRadius(Player player, int x, int y, int z) {
        Vector eye = player.getEyeLocation().toVector();
        Vector block = new Vector(x, y, z);
        return eye.distanceSquared(block) <= MAX_DIG_RADIUS * MAX_DIG_RADIUS;
    }
}