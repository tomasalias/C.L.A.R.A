package AC.Packets;

import AC.Utils.PluginUtils.PlayerOpStorage;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.ExecutorService;

/**
 * The ListenerRegistrar class is responsible for registering both packet listeners
 * (to handle network-level events) and event listeners (to handle Bukkit events) in the plugin.
 */
public final class ListenerRegistrar {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * This ensures the class is used only for its static methods.
     */
    private ListenerRegistrar() {
    }


    public static void registerPacketListeners(ExecutorService executorService, PlayerOpStorage playerOpStorage) {

        PacketEvents.getAPI().getEventManager().registerListener(new ClientPacketListener(executorService,playerOpStorage));
    }

    /**
     * Registers event listeners with the Bukkit plugin manager.
     * These listeners respond to server-side events such as player interactions.
     *
     * @param plugin    The main plugin instance required for registering events.
     * @param listeners An array of Bukkit event listeners to be registered.
     */
    public static void registerEventListeners(Plugin plugin, org.bukkit.event.Listener... listeners) {
        for (org.bukkit.event.Listener listener : listeners) {
            // Register each listener with the server's plugin manager
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}