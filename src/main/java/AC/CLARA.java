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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CLARA extends JavaPlugin {

    @Getter private static CLARA instance;
    @Getter private PlayerOpStorage playerOpStorage;
    @Getter private ConcurrentHashMap<UUID, Long> playerRespawnMap;
    @Getter public Timer timer;
    private ExecutorService executorService;
    public static ConcurrentHashMap<UUID, PlayerData> playerDataMap;

    @Override
    public void onEnable() {
        // Global instance
        instance = this;

        // Thread pool & data structures
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        playerOpStorage = new PlayerOpStorage();
        playerRespawnMap = new ConcurrentHashMap<>();
        playerDataMap = new ConcurrentHashMap<>();
        timer = new Timer(executorService);

        // Command registration
        this.getCommand("acping").setExecutor(new acping());

        // Startup messaging
        Messages.startUpComments();

        // 1) Register all packet-level checks
        ListenerRegistrar.registerPacketListeners(executorService, playerOpStorage);
        ServerListenerRegistrar.registerServerPacketListeners(executorService);

        // 2) InteractEntity listener for boat click exemptions
        InteractEntity interactEntity = new InteractEntity(playerOpStorage);
        PacketEvents.getAPI().getEventManager().registerListener(interactEntity);

        // 3) Register event listeners and inject all required resources
        ListenerRegistrar.registerEventListeners(
                this,
                new PlayerInitialisers(
                        playerOpStorage,
                        executorService,
                        playerRespawnMap                       // ✅ Pass respawn map
                )
        );

        // 4) Respawn tracking — stores last respawn timestamps
        getServer().getPluginManager().registerEvents(
                new RespawnListener(playerRespawnMap),
                this
        );
    }

    @Override
    public void onDisable() {
        Messages.shutdownComments();
        PacketEvents.getAPI().getEventManager().unregisterAllListeners();

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public static PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }
}