package com.jonahseguin.payload.command;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.command.commands.*;
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
        register(new CmdDebug());
        register(new CmdProfile());
        register(new CmdSaveall());
        register(new CmdServer());
        register(new CmdSetID());
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
                    args = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    args = new String[]{};
                }
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
                if (args.length < command.minArgs()) {
                    sender.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.COMMAND_INCORRECT_USAGE, command.minArgs() + "", "/" + command.name() + " " + command.usage()));
                    return true;
                }
                CmdArgs cmdArgs = new CmdArgs(sender, pCmd, args);
                try {
                    command.execute(cmdArgs);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (PayloadPermission.ADMIN.has(sender)) {
                        cmdArgs.msg("&cPayload: Error executing command '{0}': {1}", label, ex.getMessage());
                    } else {
                        cmdArgs.msg("&cPayload: An error occurred while executing that command.  Please notify an administrator.");
                    }
                }
            } else {
                sender.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.UNKNOWN_COMMAND, pCmd));
            }

            return true;
        }
        return false;
    }
}
