package AC.Utils.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RespawnListener implements Listener {
    private final ConcurrentHashMap<UUID, Long> respawnMap;

    public RespawnListener(ConcurrentHashMap<UUID, Long> respawnMap) {
        this.respawnMap = respawnMap;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        respawnMap.put(playerUUID, System.currentTimeMillis()); // You could store other metadata here too
    }
}