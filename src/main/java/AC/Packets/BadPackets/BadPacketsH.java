package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

/**
 * Utility class for validating movement input values from steering packets.
 * Ensures that forward and sideways movement values are finite and within acceptable bounds.
 */
@UtilityClass
public class BadPacketsH {

    // Define the maximum allowed movement value.
    // This is based on typical player movement input, where values range from -1.0 to 1.0.
    // We use 0.98f as a safety threshold to catch abnormal or potentially spoofed inputs.
    private static final float MAX_VALID_MOVEMENT = 0.98f;

    /**
     * Validates the movement input values from a steering packet.
     *
     * @param forward  Forward movement input (positive = forward, negative = backward)
     * @param sideways Sideways movement input (positive = right, negative = left)
     * @return true if both values are finite and within the allowed movement threshold; false otherwise
     */
    public boolean isValidSteerMovement(float forward, float sideways) {
        // Check that both movement values are finite (not NaN or Infinity)
        // and that their absolute values do not exceed the maximum allowed threshold.
        return Float.isFinite(forward) && Float.isFinite(sideways) &&
                Math.abs(forward) <= MAX_VALID_MOVEMENT &&
                Math.abs(sideways) <= MAX_VALID_MOVEMENT;
    }
}