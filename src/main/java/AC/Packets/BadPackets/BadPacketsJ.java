package AC.Packets.BadPackets;

import AC.CLARA;
import AC.Checks.Combat.ReachCheckB;
import AC.Utils.CheckUtils.PlayerData;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

@UtilityClass
public class BadPacketsJ {

    private static final int MIN_STATUS_VALUE = 0;
    private static final int MAX_STATUS_VALUE = 6;
    private static final int MIN_FACE_VALUE = 0;
    private static final int MAX_FACE_VALUE = 5;
    private static final int MIN_VALID_POSITION = -30_000_000;
    private static final int MAX_VALID_POSITION = 30_000_000;
    private static final double MAX_DIG_RADIUS = 6; // 5.74456

    public boolean isValid(Player player, int locationX, int locationY, int locationZ, BlockFace blockFace, DiggingAction action, int status) {
        if (status < MIN_STATUS_VALUE || status > MAX_STATUS_VALUE) {
            return false;
        }

        long ping = (long) CLARA.getPlayerData(player.getUniqueId()).getCurrentPing();
        Vector sampledPosition = getSampledPlayerPosition(player.getUniqueId(), ping);
        if (sampledPosition == null) {
            return false;
        }

        // Always perform reach check early for highest accuracy
        ReachCheckB.perform(player, sampledPosition, locationX, locationY, locationZ);

        switch (status) {
            case 0: // Start digging
            case 1: // Cancel digging
            case 2: // Finish digging
                if (!isWithinCoordinateBounds(locationX, locationY, locationZ) || !isWithinFaceBounds(blockFace)) {
                    return false;
                }
                if (!isActionExemptFromRadiusCheck(action)) {
                    return isWithinDiggingRadius(player, locationX, locationY, locationZ);
                }
                return true;

            case 3: // Drop item stack
            case 4: // Drop item
            case 5: // Shoot arrow / finish eating
            case 6: // Swap item in hand
                return isValidExemptAction(locationX, locationY, locationZ, blockFace);

            default:
                return false;
        }
    }

    private Vector getSampledPlayerPosition(UUID uuid, long timeAgo) {
        PlayerData data = CLARA.getPlayerData(uuid);
        if (data == null) return null;

        var pos = data.getPositionAtTime(timeAgo);
        if (pos == null) return null;

        return new Vector(pos.getX(), pos.getY() + 1.62, pos.getZ());
    }

    private boolean isActionExemptFromRadiusCheck(DiggingAction action) {
        return action == DiggingAction.RELEASE_USE_ITEM
                || action == DiggingAction.DROP_ITEM
                || action == DiggingAction.DROP_ITEM_STACK
                || action == DiggingAction.SWAP_ITEM_WITH_OFFHAND;
    }

    private boolean isValidExemptAction(int x, int y, int z, BlockFace blockFace) {
        return x == 0 && y == 0 && z == 0 && blockFace == BlockFace.DOWN;
    }

    private boolean isWithinFaceBounds(BlockFace blockFace) {
        int value = blockFace.getFaceValue();
        return value >= MIN_FACE_VALUE && value <= MAX_FACE_VALUE;
    }

    private boolean isWithinCoordinateBounds(int x, int y, int z) {
        return x >= MIN_VALID_POSITION && x <= MAX_VALID_POSITION
                && y >= MIN_VALID_POSITION && y <= MAX_VALID_POSITION
                && z >= MIN_VALID_POSITION && z <= MAX_VALID_POSITION;
    }

    private boolean isWithinDiggingRadius(Player player, int x, int y, int z) {
        Vector eye = player.getEyeLocation().toVector();
        Vector block = new Vector(x, y, z);
        return eye.distanceSquared(block) <= MAX_DIG_RADIUS * MAX_DIG_RADIUS;
    }
}