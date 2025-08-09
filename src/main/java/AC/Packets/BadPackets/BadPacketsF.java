package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;

/**
 * Utility class for validating inventory slot indices related to bad packet detection.
 *
 * This class provides a static method to check whether a given inventory slot index
 * falls within the expected valid range. It's typically used to detect malformed or
 * suspicious packet data that references invalid inventory slots.
 */
@UtilityClass
public class BadPacketsF {

    // Minimum valid inventory slot index (inclusive)
    private static final int MIN_INVENTORY_SLOT = 0;

    // Maximum valid inventory slot index (inclusive)
    private static final int MAX_INVENTORY_SLOT = 8;

    /**
     * Checks whether the given inventory slot index is within the valid range.
     *
     * This method is used to validate packet data that references inventory slots.
     * If a packet references a slot outside this range, it may be malformed or malicious.
     *
     * @param slot The inventory slot index to validate.
     * @return true if the slot is valid (between 0 and 8 inclusive), false otherwise.
     */
    public boolean isValid(int slot) {
        return slot >= MIN_INVENTORY_SLOT && slot <= MAX_INVENTORY_SLOT;
    }
}