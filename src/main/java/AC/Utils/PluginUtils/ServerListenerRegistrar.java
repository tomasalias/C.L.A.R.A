package AC.Utils.PluginUtils;

import com.github.retrooper.packetevents.PacketEvents;

import java.util.concurrent.ExecutorService;

public final class ServerListenerRegistrar {

    private ServerListenerRegistrar() {
        // Utility class: no instantiation
    }

    /**
     * Registers server-sent packet listeners.
     * This is specifically for packets originating from the server (e.g. velocity updates),
     * allowing server-side observability and validation on outbound flows.
     */
    public static void registerServerPacketListeners(ExecutorService executorService) {

    }
}