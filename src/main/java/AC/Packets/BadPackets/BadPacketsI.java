package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Utility class for validating block interaction packets.
 * Ensures that face, cursor coordinates, and directional alignment are valid and consistent.
 */
@UtilityClass
public class BadPacketsI {

    // Cursor coordinates must be between 0.0 and 1.0 (relative to block face)
    private static final float MIN_VALID_COORDINATE = 0.0f;
    private static final float MAX_VALID_COORDINATE = 1.0f;

    // Dot product range for directional alignment check
    // Valid dot product between player's look direction and block face normal is [-1.0, 1.0]
    private static final double MIN_DOT_PRODUCT = -1.0;
    private static final double MAX_DOT_PRODUCT = 1.0;

    /**
     * Validates block interaction data from a packet.
     *
     * @param player      The player performing the interaction
     * @param face        The block face being interacted with (0–5)
     * @param cursorX     X-coordinate of the cursor on the block face (0.0–1.0)
     * @param cursorY     Y-coordinate of the cursor on the block face (0.0–1.0)
     * @param cursorZ     Z-coordinate of the cursor on the block face (0.0–1.0)
     * @param insideBlock Whether the interaction is inside the block
     * @param sequence    Sequence number of the interaction packet
     * @return true if all parameters are valid and consistent; false otherwise
     */
    public boolean isValid(Player player, Integer face, Float cursorX, Float cursorY, Float cursorZ, Boolean insideBlock, Integer sequence) {
        StringBuilder nullParams = new StringBuilder();
        boolean allNull = true;

        // Check for null values and track which parameters are missing
        if (face == null) {
            nullParams.append("face ");
        } else {
            allNull = false;
        }

        if (cursorX == null) {
            nullParams.append("cursorX ");
        } else {
            allNull = false;
        }

        if (cursorY == null) {
            nullParams.append("cursorY ");
        } else {
            allNull = false;
        }

        if (cursorZ == null) {
            nullParams.append("cursorZ ");
        } else {
            allNull = false;
        }

        if (insideBlock == null) {
            nullParams.append("insideBlock ");
        } else {
            allNull = false;
        }

        if (sequence == null) {
            nullParams.append("sequence ");
        } else {
            allNull = false;
        }

        // If all parameters are null, we assume it's a harmless or empty packet
        if (allNull) {
            return true;
        }

        // If some parameters are missing but not all, reject the packet
        if (!nullParams.isEmpty()) {
            return false;
        }

        // Validate block face index (must be between 0 and 5)
        if (!isValidFace(face)) {
            return false;
        }

        // Validate cursor coordinates (must be finite and within 0.0–1.0)
        if (!isValidCoordinates(cursorX, cursorY, cursorZ)) {
            return false;
        }

        // Validate that the player is looking in the general direction of the block face
        if (!isLookingAtCorrectFace(player, face)) {
            return false;
        }

        // All checks passed
        return true;
    }

    /**
     * Checks if the block face index is valid.
     * Valid faces: 0 = bottom, 1 = top, 2 = north, 3 = south, 4 = west, 5 = east
     */
    private boolean isValidFace(int face) {
        return face >= 0 && face <= 5;
    }

    /**
     * Checks if the player is generally facing the block face being interacted with.
     * Uses dot product between player's look direction and block face normal vector.
     */
    private boolean isLookingAtCorrectFace(Player player, int face) {
        Vector playerDirection = player.getEyeLocation().getDirection().normalize();
        Vector blockFaceDirection = getBlockFaceDirection(face);

        if (blockFaceDirection == null) {
            return false;
        }

        double dot = playerDirection.dot(blockFaceDirection);

        // Dot product must be within [-1.0, 1.0] to be considered valid
        return dot >= MIN_DOT_PRODUCT && dot <= MAX_DOT_PRODUCT;
    }

    /**
     * Maps block face index to its corresponding normal vector.
     * These vectors represent the direction perpendicular to each block face.
     */
    private Vector getBlockFaceDirection(int face) {
        return switch (face) {
            case 0 -> new Vector(0, -1, 0); // Bottom
            case 1 -> new Vector(0, 1, 0);  // Top
            case 2 -> new Vector(0, 0, -1); // North
            case 3 -> new Vector(0, 0, 1);  // South
            case 4 -> new Vector(-1, 0, 0); // West
            case 5 -> new Vector(1, 0, 0);  // East
            default -> null;               // Invalid face index
        };
    }

    /**
     * Validates that cursor coordinates are finite and within the 0.0–1.0 range.
     * These values represent the relative position of the interaction on the block face.
     */
    private boolean isValidCoordinates(float cursorX, float cursorY, float cursorZ) {
        return Float.isFinite(cursorX) && Float.isFinite(cursorY) && Float.isFinite(cursorZ) &&
                cursorX >= MIN_VALID_COORDINATE && cursorX <= MAX_VALID_COORDINATE &&
                cursorY >= MIN_VALID_COORDINATE && cursorY <= MAX_VALID_COORDINATE &&
                cursorZ >= MIN_VALID_COORDINATE && cursorZ <= MAX_VALID_COORDINATE;
    }
}