package AC.Commands;

import AC.CLARA;
import AC.Utils.CheckUtils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class acping implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /acping <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        PlayerData targetData = CLARA.getPlayerData(targetUUID);

        if (targetData == null) {
            sender.sendMessage(ChatColor.YELLOW + "No ping data available for player '" + target.getName() + "'.");
            return true;
        }

        double ping = targetData.getLastAveragePing();
        sender.sendMessage(ChatColor.GREEN + target.getName() + "'s ping is " + ChatColor.AQUA + Math.round(ping) + "ms.");
        return true;
    }
}
