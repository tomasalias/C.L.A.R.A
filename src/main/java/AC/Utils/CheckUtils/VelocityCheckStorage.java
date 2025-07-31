package AC.Utils.CheckUtils;

import AC.Checks.Movement.VelocityCheckA;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityCheckStorage {

    private static final ConcurrentHashMap<UUID, VelocityCheckA> velocityChecks = new ConcurrentHashMap<>();

    public static void registerPlayer(UUID uuid) {
        velocityChecks.put(uuid, new VelocityCheckA(uuid));
    }

    public static VelocityCheckA get(UUID uuid) {
        return velocityChecks.get(uuid);
    }

    public static void unregisterPlayer(UUID uuid) {
        velocityChecks.remove(uuid);
    }
}