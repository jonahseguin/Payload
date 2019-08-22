package com.jonahseguin.payload.command;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
public class CmdArgs {

    private final CommandSender sender;
    private final String command;
    private final String[] args;

    public CmdArgs(CommandSender sender, String command, String[] args) {
        this.sender = sender;
        this.command = command;
        this.args = args;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player getPlayer() {
        if (this.isPlayer()) {
            return (Player) sender;
        }
        throw new ClassCastException("The sender is not a player, check if isPlayer first (Payload command: /" + command + ")");
    }

    public void msg(String msg) {
        this.sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

}
