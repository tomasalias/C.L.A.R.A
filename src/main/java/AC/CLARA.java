package AC;

import AC.Checks.Movement.SpeedCheckA;
import AC.Commands.acping;
import AC.Utils.CheckUtils.PlayerData;
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
    // Provide a static method to retrieve the plugin instance
    @Getter
    private static CLARA instance; // Static instance of the plugin for global access
    // Provide access to PlayerOpStorage for other components of the plugin
    @Getter
    private PlayerOpStorage playerOpStorage; // Manages player operation data
    // Provide access to SpeedCheckMap for other components of the plugin
    @Getter
    private ConcurrentHashMap<UUID, SpeedCheckA> speedCheckMap; // Tracks speed checks for individual players
    private ExecutorService executorService; // Thread pool for managing asynchronous tasks
    public static ConcurrentHashMap<UUID, PlayerData> playerDataMap;
    public static PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }
    private final ConcurrentHashMap<String, Long> playerPingTimestamps = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // Set the static instance of the plugin
        instance = this;

        // Initialize the thread pool to use all available processors
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Initialize PlayerOpStorage to store player operator data.
        playerOpStorage = new PlayerOpStorage();

        // Initialize the ConcurrentHashMap to manage SpeedCheckA instances for players
        speedCheckMap = new ConcurrentHashMap<>();

        // Register the /acping command
        this.getCommand("acping").setExecutor(new acping());



        // Display startup messages
        Messages.startUpComments();

        // Register packet listeners
        ListenerRegistrar.registerPacketListeners(speedCheckMap, executorService,playerOpStorage);

        // Register event listeners and pass dependencies (PlayerOpStorage, SpeedCheckMap, and ThreadPool)
        ListenerRegistrar.registerEventListeners(this, new PlayerInitialisers(playerOpStorage, speedCheckMap, executorService));

        playerDataMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onDisable() {
        // Display shutdown messages
        Messages.shutdownComments();

        // Unregister all packet listeners
        PacketEvents.getAPI().getEventManager().unregisterAllListeners();

        // Shut down the executor service to release resources
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();

            // Trigger shutdown for each SpeedCheckA instance which prevents the speedcheck logic from running.
            for (SpeedCheckA speedCheckA : speedCheckMap.values()) {
                speedCheckA.SpeedCheckAShutdown();  // Trigger shutdown for SpeedCheckA
            }
        }
    }

}