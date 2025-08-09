package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

/**
 * Utility class for validating player identity data in packets.
 * Ensures that both the player name and UUID follow expected formats.
 */
@UtilityClass
public class BadPacketsK {

    // Regular expression pattern for validating UUID format.
    // Matches standard UUIDs like: 123e4567-e89b-12d3-a456-426614174000
    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    // Regular expression pattern for validating Minecraft usernames.
    // Valid usernames are 1 to 16 characters long and may contain letters, numbers, and underscores.
    private static final String PLAYER_NAME_PATTERN = "^[a-zA-Z0-9_]{1,16}$";

    /**
     * Validates the player's name and UUID.
     *
     * @param playerName The player's username
     * @param uuid       The player's UUID in string format
     * @return true if both values are non-null and match their respective patterns; false otherwise
     */
    public boolean isValid(String playerName, String uuid) {
        // Check that playerName is not null and matches the allowed username pattern
        // Check that uuid is not null and matches the standard UUID format
        return playerName != null && playerName.matches(PLAYER_NAME_PATTERN) &&
                uuid != null && uuid.matches(UUID_PATTERN);
    }
}