package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

/**
 * Utility class for validating packet position and orientation data.
 * Ensures that coordinates and angles are within acceptable bounds and are finite values.
 */
@UtilityClass
public class BadPacketsG {

    // Define the minimum and maximum valid coordinate values.
    private static final int MIN_VALID_POSITION = -30000000;
    private static final double MAX_VALID_POSITION = 30000000;

    // Define the valid pitch range in degrees (-90 to +90).
    private static final float MIN_VALID_PITCH = -90.0f;
    private static final float MAX_VALID_PITCH = 90.0f;

    // Define the valid yaw range in degrees (0 to 360).
    private static final float MIN_VALID_YAW = 0.0f;
    private static final float MAX_VALID_YAW = 360.0f;

    /**
     * Validates the given position and orientation values.
     *
     * @param x     X-coordinate of the entity/player
     * @param y     Y-coordinate (height) of the entity/player
     * @param z     Z-coordinate of the entity/player
     * @param yaw   Horizontal rotation (0 to 360 degrees)
     * @param pitch Vertical rotation (-90 to +90 degrees)
     * @return true if all values are finite and within bounds; false otherwise
     */
    public static boolean isValid(double x, double y, double z, float yaw, float pitch) {
        System.out.println("[DEBUG] Starting validation for position and orientation.");
        System.out.println("[DEBUG] Received values -> x: " + x + ", y: " + y + ", z: " + z + ", yaw: " + yaw + ", pitch: " + pitch);

        // Check if all values are finite (not NaN or Infinity)
        boolean xFinite = Double.isFinite(x);
        boolean yFinite = Double.isFinite(y);
        boolean zFinite = Double.isFinite(z);
        boolean yawFinite = Float.isFinite(yaw);
        boolean pitchFinite = Float.isFinite(pitch);

        System.out.println("[DEBUG] Finite checks -> x: " + xFinite + ", y: " + yFinite + ", z: " + zFinite + ", yaw: " + yawFinite + ", pitch: " + pitchFinite);

        if (!xFinite || !yFinite || !zFinite || !yawFinite || !pitchFinite) {
            System.out.println("[ERROR] Invalid position or orientation: Non-finite value detected.");
            return false;
        }

        // Check if the values fall within the defined bounds
        boolean withinBounds = isWithinBounds(x, y, z, yaw, pitch);
        System.out.println("[DEBUG] Bounds check result: " + withinBounds);

        if (!withinBounds) {
            System.out.println("[ERROR] Out of bounds: x=" + x + ", y=" + y + ", z=" + z + ", yaw=" + yaw + ", pitch=" + pitch);
            return false;
        }

        System.out.println("[DEBUG] Validation passed.");
        return true;
    }

    /**
     * Checks whether the given coordinates and orientation angles are within the allowed bounds.
     *
     * @param x     X-coordinate
     * @param y     Y-coordinate
     * @param z     Z-coordinate
     * @param yaw   Horizontal rotation (0 to 360)
     * @param pitch Vertical rotation (-90 to +90)
     * @return true if all values are within bounds; false otherwise
     */
    private static boolean isWithinBounds(double x, double y, double z, float yaw, float pitch) {
        boolean xValid = x >= MIN_VALID_POSITION && x <= MAX_VALID_POSITION;
        boolean yValid = y >= MIN_VALID_POSITION && y <= MAX_VALID_POSITION;
        boolean zValid = z >= MIN_VALID_POSITION && z <= MAX_VALID_POSITION;
        boolean pitchValid = pitch >= MIN_VALID_PITCH && pitch <= MAX_VALID_PITCH;
        boolean yawValid = yaw >= MIN_VALID_YAW && yaw <= MAX_VALID_YAW;

        System.out.println("[DEBUG] Bound checks -> x: " + xValid + ", y: " + yValid + ", z: " + zValid + ", pitch: " + pitchValid + ", yaw: " + yawValid);

        return xValid && yValid && zValid && pitchValid && yawValid;
    }
}