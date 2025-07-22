package AC.Packets.BadPackets;

import org.bukkit.entity.Player;
import lombok.experimental.UtilityClass;

/**
 * Utility class for validating player position and orientation data in network packets.
 * Ensures coordinates and rotation values are finite and within acceptable ranges.
 * Operators are exempted from these checks to prevent false positives during moderation.
 */
@UtilityClass
public class BadPacketsA {

    private static final int MIN_VALID_POSITION = -30_000_000;
    private static final double MAX_VALID_POSITION = 30_000_000;
    private static final float MIN_VALID_PITCH = -90.0f;
    private static final float MAX_VALID_PITCH = 90.0f;
    private static final int MIN_VALID_YAW = 0;
    private static final int MAX_VALID_YAW = 360;

    /**
     * Validates packet data for position and orientation.
     * Returns true if player is exempt or values are all within bounds.
     *
     * @param player The player associated with the packet.
     * @param x      Player's X coordinate.
     * @param y      Player's Y coordinate.
     * @param z      Player's Z coordinate.
     * @param yaw    Player's yaw (horizontal rotation).
     * @param pitch  Player's pitch (vertical rotation).
     * @return True if valid or exempt, false if malformed.
     */
    public static boolean isValid(Player player, double x, double y, double z, float yaw, float pitch) {
        if (player != null && player.isOp()) {
            return true; // Operators bypass validation
        }

        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) &&
                Float.isFinite(yaw) && Float.isFinite(pitch) &&
                isWithinBounds(x, y, z, yaw, pitch);
    }

    /**
     * Internal bounds verification method.
     *
     * @param x     X position.
     * @param y     Y position.
     * @param z     Z position.
     * @param yaw   Yaw angle.
     * @param pitch Pitch angle.
     * @return True if all values fall within valid ranges.
     */
    private static boolean isWithinBounds(double x, double y, double z, float yaw, float pitch) {
        return x >= MIN_VALID_POSITION && x <= MAX_VALID_POSITION &&
                y >= MIN_VALID_POSITION && y <= MAX_VALID_POSITION &&
                z >= MIN_VALID_POSITION && z <= MAX_VALID_POSITION &&
                pitch >= MIN_VALID_PITCH && pitch <= MAX_VALID_PITCH &&
                yaw >= MIN_VALID_YAW && yaw <= MAX_VALID_YAW;
    }
}