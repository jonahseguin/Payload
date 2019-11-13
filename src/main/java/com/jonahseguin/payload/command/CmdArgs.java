/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command;

import com.jonahseguin.payload.PayloadPlugin;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

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

    public String arg(int index) {
        return this.args[index];
    }

    public int length() {
        return args.length;
    }

    public String joinArgs(int start, int end) {
        String[] args = Arrays.copyOfRange(this.args, start, end);
        StringBuilder s = new StringBuilder();
        for (String arg : args) {
            s.append(arg).append(" ");
        }
        if (s.length() > 0) {
            s = new StringBuilder(s.substring(0, s.length() - 1));
        }
        return s.toString();
    }

    public String joinArgs() {
        return this.joinArgs(0, this.length());
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

    public void msg(String msg, Object... args) {
        this.sender.sendMessage(PayloadPlugin.format(msg, args));
    }

}
