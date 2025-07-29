package AC;

import AC.Checks.Movement.SpeedCheckA;
import AC.Commands.acping;
import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.Listeners.RespawnListener;
import AC.Utils.PluginUtils.ListenerRegistrar;
import AC.Utils.PluginUtils.Messages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CLARA extends JavaPlugin {
    @Getter
    private static CLARA instance;

    @Getter
    private PlayerOpStorage playerOpStorage;

    @Getter
    private ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap;

    @Getter
    private ConcurrentHashMap<UUID, Long> playerRespawnMap;

    private ExecutorService executorService;

    public static ConcurrentHashMap<UUID, PlayerData> playerDataMap;

    public static PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    private final ConcurrentHashMap<String, Long> playerPingTimestamps = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // Set instance reference for global access
        instance = this;

        // Initialize thread pool
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Initialize maps
        playerOpStorage = new PlayerOpStorage();
        speedCheckMap = new ConcurrentHashMap<>();
        playerRespawnMap = new ConcurrentHashMap<>();
        playerDataMap = new ConcurrentHashMap<>();

        // Register commands
        this.getCommand("acping").setExecutor(new acping());

        // Startup message
        Messages.startUpComments();

        // Register core listeners
        ListenerRegistrar.registerPacketListeners(speedCheckMap, executorService, playerOpStorage);
        ListenerRegistrar.registerEventListeners(this, new PlayerInitialisers(playerOpStorage, speedCheckMap, executorService));

        // Register respawn tracking listener
        getServer().getPluginManager().registerEvents(new RespawnListener(playerRespawnMap), this);
    }

    @Override
    public void onDisable() {
        // Shutdown message
        Messages.shutdownComments();

        // Unregister listeners
        PacketEvents.getAPI().getEventManager().unregisterAllListeners();

        // Gracefully shut down thread pool
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();

            for (SpeedCheckA speedCheckA : speedCheckMap.values()) {
                speedCheckA.SpeedCheckAShutdown();
            }
        }
    }
}