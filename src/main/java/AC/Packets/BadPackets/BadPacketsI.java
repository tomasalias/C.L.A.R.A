package AC.Packets.BadPackets;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@UtilityClass
public class BadPacketsI {

    private static final float MIN_VALID_COORDINATE = 0.0f;
    private static final float MAX_VALID_COORDINATE = 1.0f;
    private static final double MIN_DOT_PRODUCT = -1.0;
    private static final double MAX_DOT_PRODUCT = 1.0;

    public boolean isValid(Player player, Integer face, Float cursorX, Float cursorY, Float cursorZ, Boolean insideBlock, Integer sequence) {
        StringBuilder nullParams = new StringBuilder();
        boolean allNull = true;

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

        if (allNull) {
            return true;
        }

        if (!nullParams.isEmpty()) {
            return false;
        }

        if (!isValidFace(face)) {
            return false;
        }

        if (!isValidCoordinates(cursorX, cursorY, cursorZ)) {
            return false;
        }

        if (!isLookingAtCorrectFace(player, face)) {
            return false;
        }

        return true;
    }

    private boolean isValidFace(int face) {
        return face >= 0 && face <= 5;
    }

    private boolean isLookingAtCorrectFace(Player player, int face) {
        Vector playerDirection = player.getEyeLocation().getDirection().normalize();
        Vector blockFaceDirection = getBlockFaceDirection(face);

        if (blockFaceDirection == null) {
            return false;
        }

        double dot = playerDirection.dot(blockFaceDirection);
        return dot >= MIN_DOT_PRODUCT && dot <= MAX_DOT_PRODUCT;
    }

    private Vector getBlockFaceDirection(int face) {
        return switch (face) {
            case 0 -> new Vector(0, -1, 0); // Bottom
            case 1 -> new Vector(0, 1, 0);  // Top
            case 2 -> new Vector(0, 0, -1); // North
            case 3 -> new Vector(0, 0, 1);  // South
            case 4 -> new Vector(-1, 0, 0); // West
            case 5 -> new Vector(1, 0, 0);  // East
            default -> null;
        };
    }

    private boolean isValidCoordinates(float cursorX, float cursorY, float cursorZ) {
        return Float.isFinite(cursorX) && Float.isFinite(cursorY) && Float.isFinite(cursorZ) &&
                cursorX >= MIN_VALID_COORDINATE && cursorX <= MAX_VALID_COORDINATE &&
                cursorY >= MIN_VALID_COORDINATE && cursorY <= MAX_VALID_COORDINATE &&
                cursorZ >= MIN_VALID_COORDINATE && cursorZ <= MAX_VALID_COORDINATE;
    }
}