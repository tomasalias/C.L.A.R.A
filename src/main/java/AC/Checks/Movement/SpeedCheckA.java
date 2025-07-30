package AC.Checks.Movement;

import AC.Utils.CheckUtils.FastMath;
import AC.Utils.PluginUtils.KickMessages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class SpeedCheckA {

    private static final double DEFAULT_THRESHOLD = 0.5;
    private static final double VERTICAL_ADJUSTED_THRESHOLD = 0.8;
    private static final long BOAT_EXEMPTION_WINDOW_MS = 2000;

    @Getter
    private final UUID playerUUID;

    private Vector3D positionA = null;
    private Vector3D positionB = null;
    private boolean processing = false;

    private final ExecutorService executorService;
    private Boolean isOpCached = null;

    private final PlayerOpStorage playerOpStorage;
    private final Function<UUID, Boolean> boatExemptionProvider;
    private final ConcurrentHashMap<UUID, Long> respawnMap;

    public SpeedCheckA(UUID playerUUID,
                       PlayerOpStorage playerOpStorage,
                       ExecutorService executorService,
                       Function<UUID, Boolean> boatExemptionProvider,
                       ConcurrentHashMap<UUID, Long> respawnMap) {
        this.playerUUID = playerUUID;
        this.playerOpStorage = playerOpStorage;
        this.executorService = executorService;
        this.boatExemptionProvider = boatExemptionProvider;
        this.respawnMap = respawnMap;
    }

    public void handlePosition(Player player, double x, double y, double z) {
        if (isOpCached == null) {
            isOpCached = playerOpStorage.isPlayerOperator(player);
        }

        if (isOpCached || processing) {
            return;
        }

        if (boatExemptionProvider.apply(playerUUID)) {
            positionA = null;
            System.out.println("[SpeedCheckA] Skipping movement check due to boat exemption.");
            return;
        }

        processing = true;

        executorService.submit(() -> {
            if (positionA == null) {
                positionA = new Vector3D(x, y, z);
                processing = false;
                return;
            }

            Vector3D currentPosition = new Vector3D(x, y, z);
            if (FastMath.areVectorsApproximatelyEqual(positionA, currentPosition)) {
                processing = false;
                return;
            }

            positionB = currentPosition;
            Vector3D delta = FastMath.calculateDelta(positionA, positionB);

            double deltaX = delta.getX();
            double deltaY = delta.getY();
            double deltaZ = delta.getZ();

            boolean changeInY = Math.abs(deltaY) > 0.000001;
            double threshold = changeInY ? VERTICAL_ADJUSTED_THRESHOLD : DEFAULT_THRESHOLD;
            double horizontalDelta = Math.max(Math.abs(deltaX), Math.abs(deltaZ));

            double speedPercentage = FastMath.calculateSpeedPercentage(horizontalDelta, threshold);
            double speedDeviation = speedPercentage - 100.0;

            if (horizontalDelta > threshold) {
                boolean exempt = boatExemptionProvider.apply(playerUUID);
                if (exempt) {
                    System.out.println("[SpeedCheckA] Exempting movement spike due to recent boat click.");
                } else {
                    try {
                        Thread.sleep(BOAT_EXEMPTION_WINDOW_MS);
                    } catch (InterruptedException ignored) {}

                    // NEW: Exemption due to recent respawn
                    if (respawnMap.containsKey(playerUUID)) {
                        System.out.println("[SpeedCheckA] Post-delay exemption due to recent respawn — skipping punishment.");
                        processing = false;
                        return;
                    }

                    // Final boat exemption check after delay
                    if (!boatExemptionProvider.apply(playerUUID)) {
                        KickMessages.kickPlayerForSpeedCheck(
                                player,
                                String.format("Speed: %+1.2f%%", speedDeviation)
                        );
                    } else {
                        System.out.println("[SpeedCheckA] Post-delay exemption held — skipping punishment.");
                    }
                }
            }

            positionA = null;
            positionB = null;
            processing = false;
        });
    }

    public void SpeedCheckAShutdown() {
        processing = true;
    }
}