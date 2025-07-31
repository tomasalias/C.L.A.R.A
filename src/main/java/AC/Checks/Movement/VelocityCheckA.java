package AC.Checks.Movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class VelocityCheckA {

    private final UUID playerUUID;
    private static final Logger logger = Logger.getLogger("AC");
    private final List<Vector3d> recentPositions = new ArrayList<>();

    public VelocityCheckA(UUID playerUUID) {
        this.playerUUID = playerUUID;
        logger.info("VelocityCheckA instantiated for: " + playerUUID);
    }

    public void logTrigger() {
        logger.info("VelocityCheckA is working for: " + playerUUID);
    }

    public void addPosition(double x, double y, double z) {
        recentPositions.add(new Vector3d(x, y, z));
        logger.info("VelocityCheckA received position: " + x + ", " + y + ", " + z);
    }

    public void EntityVelocitySent(Player player, Vector3d velocity, int entityId) {
        logger.info("VelocityCheckA captured velocity for " + player.getName() +
                " → X: " + velocity.x + ", Y: " + velocity.y + ", Z: " + velocity.z +
                " [Entity ID: " + entityId + "]");

    }

}