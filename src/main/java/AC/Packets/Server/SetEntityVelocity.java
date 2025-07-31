package AC.Packets.Server;

import AC.Checks.Movement.VelocityCheckA;
import AC.Utils.CheckUtils.VelocityCheckStorage;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

public class SetEntityVelocity extends PacketListenerAbstract {

    private static final Logger logger = Logger.getLogger("AC");

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            handleEntityVelocity(event.getPlayer(), event);
        }
    }

    public void handleEntityVelocity(Player player, PacketSendEvent event) {
        UUID playerUUID = player.getUniqueId();
        WrapperPlayServerEntityVelocity velocityWrapper = new WrapperPlayServerEntityVelocity(event);

        velocityWrapper.read();

        int entityId = velocityWrapper.getEntityId();
        Vector3d velocity = velocityWrapper.getVelocity();

        logger.info("Server sent velocity to entityID " + entityId + " (" + player.getName() + ") → " +
                "X: " + velocity.x + ", Y: " + velocity.y + ", Z: " + velocity.z);

        VelocityCheckA velocityCheck = VelocityCheckStorage.get(playerUUID);
        if (velocityCheck != null) {
            velocityCheck.EntityVelocitySent(player, velocity, entityId);
        }
    }
}