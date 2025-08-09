package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;


// Utility class for validating player orientation data, specifically yaw and pitch.
// This class ensures that yaw (horizontal rotation) and pitch (vertical rotation) values
// are within realistic and valid bounds. Validating these values helps detect potential
// exploits or malformed packets that could disrupt gameplay.

@UtilityClass
public class BadPacketsC {

    // Constants defining the valid range for pitch (up and down rotation).
    // In Minecraft, pitch values represent angles, with -90 being straight up
    // and 90 being straight down.
    private static final float MIN_VALID_PITCH = -90.0f; // Minimum valid pitch angle
    private static final float MAX_VALID_PITCH = 90.0f;  // Maximum valid pitch angle

    // Constants defining the valid range for yaw (horizontal rotation).
    // Yaw values represent angles, with 0 representing north and increasing clockwise.
    private static final int Min_Valid_Yaw = 0;    // Minimum valid yaw angle
    private static final int Max_Valid_Yaw = 360; // Maximum valid yaw angle

    /**
     * Validates yaw and pitch values based on two criteria:
     * 1. Ensures both yaw and pitch are finite values (not NaN or infinite).
     * 2. Ensures both values fall within their defined valid ranges.
     *
     * @param yaw   The player's yaw (horizontal rotation).
     * @param pitch The player's pitch (vertical rotation).
     * @return True if both yaw and pitch values are valid, false otherwise.
     */
    public static boolean isValid(float yaw, float pitch) {
        // Check if yaw and pitch are finite and within their respective valid ranges.
        return Float.isFinite(yaw) && Float.isFinite(pitch) &&
                pitch >= MIN_VALID_PITCH && pitch <= MAX_VALID_PITCH &&
                yaw >= Min_Valid_Yaw && yaw <= Max_Valid_Yaw;
    }
}
