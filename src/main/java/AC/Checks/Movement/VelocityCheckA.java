package AC.Checks.Movement;

import java.util.UUID;
import java.util.logging.Logger;

public class VelocityCheckA {

    private final UUID playerUUID;
    private static final Logger logger = Logger.getLogger("AC");

    public VelocityCheckA(UUID playerUUID) {
        this.playerUUID = playerUUID;
        logger.info("VelocityCheckA instantiated for: " + playerUUID);
    }

    public void logTrigger() {
        logger.info("VelocityCheckA is working for: " + playerUUID);
    }
}