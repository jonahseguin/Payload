package com.jonahseguin.payload.command;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.command.commands.CmdCache;
import com.jonahseguin.payload.command.commands.CmdCacheLayers;
import com.jonahseguin.payload.command.commands.CmdCacheList;
import com.jonahseguin.payload.command.commands.CmdHelp;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PCommandHandler implements CommandExecutor {

    private final Map<String, PayloadCommand> commands = new HashMap<>();

    public PCommandHandler() {
        register(new CmdHelp());
        register(new CmdCache());
        register(new CmdCacheList());
        register(new CmdCacheLayers());
    }

    private void register(PayloadCommand cmd) {
        this.commands.put(cmd.name().toLowerCase(), cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("payload")) {
            String pCmd = "help";
            if (args.length > 0) {
                pCmd = args[0];
                if (args.length > 1) {
                    args = Arrays.copyOfRange(args, 1, args.length - 1);
                } else {
                    args = new String[]{};
                }

                PayloadCommand command = null;

                if (commands.containsKey(pCmd.toLowerCase())) {
                    command = commands.get(pCmd.toLowerCase());
                } else {
                    for (PayloadCommand pcmds : commands.values()) {
                        if (pcmds.name().equalsIgnoreCase(pCmd)) {
                            command = pcmds;
                            break;
                        }
                        for (String alias : pcmds.aliases()) {
                            if (alias.equalsIgnoreCase(pCmd)) {
                                command = pcmds;
                                break;
                            }
                        }
                    }
                }

                if (command != null) {
                    if (command.playerOnly() && !(sender instanceof Player)) {
                        sender.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.COMMAND_PLAYER_ONLY));
                        return true;
                    }
                    if (!command.permission().has(sender)) {
                        sender.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.COMMAND_NO_PERMISSION));
                        return true;
                    }
                    CmdArgs cmdArgs = new CmdArgs(sender, pCmd, args);
                    command.execute(cmdArgs);
                } else {
                    sender.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.UNKNOWN_COMMAND, pCmd));
                }

            }

            return true;
        }
        return false;
    }
}
