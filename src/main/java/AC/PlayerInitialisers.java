package AC;

import AC.Checks.Movement.SpeedCheckA;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.CheckUtils.VelocityCheckStorage;
import AC.Utils.PluginUtils.PlayerOpStorage;
import AC.Utils.PluginUtils.sendPingPacket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static AC.CLARA.playerDataMap;

/**
 * The PlayerInitialisers class is responsible for handling player-related events
 * and managing their associated data, including movement checks.
 */
public class PlayerInitialisers implements Listener {
    private final PlayerOpStorage playerOpStorage;
    private final ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap;
    private final ExecutorService threadPool;
    private final Function<UUID, Boolean> boatExemptionProvider;

    /**
     * @param playerOpStorage       manages operator status
     * @param speedCheckMap         holds per-player SpeedCheckA instances
     * @param threadPool            executor for async tasks
     * @param boatExemptionProvider checks recent boat-click exemptions
     */
    private final ConcurrentHashMap<UUID, Long> playerRespawnMap;

    public PlayerInitialisers(
            PlayerOpStorage playerOpStorage,
            ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap,
            ExecutorService threadPool,
            Function<UUID, Boolean> boatExemptionProvider,
            ConcurrentHashMap<UUID, Long> playerRespawnMap // ✅ ADD THIS
    ) {
        this.playerOpStorage = playerOpStorage;
        this.speedCheckMap = speedCheckMap;
        this.threadPool = threadPool;
        this.boatExemptionProvider = boatExemptionProvider;
        this.playerRespawnMap = playerRespawnMap;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        playerOpStorage.updatePlayerOperatorStatus(player);

        // Construct SpeedCheckA with all dependencies including respawnMap
        SpeedCheckA speedCheckA = new SpeedCheckA(
                playerUUID,
                playerOpStorage,
                threadPool,
                boatExemptionProvider,
                playerRespawnMap // <-- Injecting the respawn exemption map
        );
        speedCheckMap.put(playerUUID, speedCheckA);
        VelocityCheckStorage.registerPlayer(playerUUID);


        playerDataMap.put(playerUUID, new PlayerData());
        sendPingPacket.triggerPing(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        speedCheckMap.remove(playerUUID);
        playerOpStorage.removePlayerOperatorStatus(player);
        VelocityCheckStorage.unregisterPlayer(playerUUID);
        PlayerData pd = playerDataMap.get(playerUUID);
        if (pd != null) pd.stopPingLogging();
        playerDataMap.remove(playerUUID);
    }
}