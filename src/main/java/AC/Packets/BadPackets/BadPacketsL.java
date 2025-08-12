package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

/**
 * Utility class for validating hand values in SWING packets.
 *
 * This class provides a static method to check whether a given hand value
 * (represented as a VarInt enum) is within the expected valid range.
 * Used to detect malformed or suspicious packet data referencing invalid hands.
 */
@UtilityClass
public class BadPacketsL {

    // Minimum valid hand value (MAIN_HAND)
    private static final int MIN_HAND_VALUE = 0;

    // Maximum valid hand value (OFF_HAND)
    private static final int MAX_HAND_VALUE = 1;

    /**
     * Checks whether the given hand value is within the valid range.
     *
     * Valid values are:
     * - 0: MAIN_HAND
     * - 1: OFF_HAND
     *
     * @param handOrdinal The ordinal value of the hand enum.
     * @return true if the hand value is valid, false otherwise.
     */
    public boolean isValid(int handOrdinal) {
        return handOrdinal >= MIN_HAND_VALUE && handOrdinal <= MAX_HAND_VALUE;
    }
}