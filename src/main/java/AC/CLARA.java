package AC;

import AC.Packets.Client.InteractEntity;
import AC.Checks.Timer;
import AC.Commands.acping;

import AC.Utils.CheckUtils.PlayerData;
import AC.Utils.Listeners.RespawnListener;
import AC.Packets.ListenerRegistrar;
import AC.Utils.PluginUtils.Messages;
import AC.Utils.PluginUtils.PlayerOpStorage;
import AC.Utils.PluginUtils.ServerListenerRegistrar;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * Main plugin class for CLARA Anti-Cheat.
 * Handles lifecycle events, thread pool setup, and listener registration.
 */
public final class CLARA extends JavaPlugin {

    // Static reference to the plugin instance for global access
    @Getter private static CLARA instance;

    // Stores operator status and exemptions per player
    @Getter private PlayerOpStorage playerOpStorage;

    // Tracks player respawn timestamps for exemption logic
    @Getter private ConcurrentHashMap<UUID, Long> playerRespawnMap;

    // Timer utility for scheduling and timing checks
    @Getter public Timer timer;

    // Thread pool used for running computationally heavy checks off the main thread
    private ExecutorService executorService;

    // Stores per-player data used by checks (e.g., ping, movement history)
    public static ConcurrentHashMap<UUID, PlayerData> playerDataMap;

    @Override
    public void onEnable() {
        // Set global plugin instance
        instance = this;

        // Create an adaptive thread pool to handle anti-cheat checks
        // This avoids lagging the main server thread by running checks concurrently
        executorService = createAdaptiveThreadPool();

        // Initialize core data structures
        playerOpStorage = new PlayerOpStorage();
        playerRespawnMap = new ConcurrentHashMap<>();
        playerDataMap = new ConcurrentHashMap<>();
        timer = new Timer(executorService);

        // Register command for ping diagnostics
        this.getCommand("acping").setExecutor(new acping());

        // Display startup messages in console
        Messages.startUpComments();

        // Register packet-level listeners for movement, rotation, and other checks
        ListenerRegistrar.registerPacketListeners(executorService, playerOpStorage);
        ServerListenerRegistrar.registerServerPacketListeners(executorService);

        // Register listener for entity interaction (e.g., boat click exemptions)
        InteractEntity interactEntity = new InteractEntity(playerOpStorage);
        PacketEvents.getAPI().getEventManager().registerListener(interactEntity);

        // Register event listeners and inject required resources
        ListenerRegistrar.registerEventListeners(
                this,
                new PlayerInitialisers(
                        playerOpStorage,
                        executorService,
                        playerRespawnMap // ✅ Used for exemption logic
                )
        );

        // Register listener to track player respawn events
        getServer().getPluginManager().registerEvents(
                new RespawnListener(playerRespawnMap),
                this
        );
    }

    @Override
    public void onDisable() {
        // Display shutdown messages in console
        Messages.shutdownComments();

        // Unregister all packet listeners
        PacketEvents.getAPI().getEventManager().unregisterAllListeners();

        // Gracefully shut down the thread pool to avoid memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Utility method to retrieve PlayerData for a given UUID.
     * Used by checks and listeners to access per-player state.
     */
    public static PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    /**
     * Creates an adaptive thread pool for running anti-cheat checks.
     * This pool scales based on available CPU cores and can handle burst load.
     *
     * Why this matters:
     * - Minecraft's main thread is single-threaded and must stay responsive.
     * - Anti-cheat checks are CPU-heavy and should run off-thread.
     * - This pool allows parallel execution without overwhelming the server.
     *
     * How it works:
     * - Starts with a number of threads equal to available processors.
     * - Can grow to double that size during high load (e.g., vCore burst).
     * - Extra threads are removed after 60 seconds of inactivity.
     * - Tasks are queued if all threads are busy, up to 1000 tasks.
     */
    private ExecutorService createAdaptiveThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors(); // Current logical CPU cores
        int maxThreads = Math.max(cores + 2, cores * 2); // Allow burst scaling

        return new ThreadPoolExecutor(
                cores, // Minimum number of threads always available
                maxThreads, // Maximum threads allowed during high load
                60L, TimeUnit.SECONDS, // Idle threads above 'core' are removed after 60s
                new LinkedBlockingQueue<>(1000) // Queue for waiting tasks
        );
    }
}