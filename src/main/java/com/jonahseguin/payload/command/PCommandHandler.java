/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.LangService;
import com.jonahseguin.payload.command.commands.*;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PCommandHandler implements CommandExecutor, LangModule {

    private final PayloadPlugin plugin;
    private final LangService langService;
    private final LangDefinitions lang;
    private final Map<String, PayloadCommand> commands = new HashMap<>();

    public PCommandHandler(@Nonnull PayloadPlugin plugin, @Nonnull LangService langService, @Nonnull Injector injector) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(langService);
        Preconditions.checkNotNull(injector);
        this.plugin = plugin;
        this.langService = langService;
        this.lang = langService.module(this);
        register(injector.getInstance(CmdHelp.class));
        register(injector.getInstance(CmdCache.class));
        register(injector.getInstance(CmdCacheList.class));
        register(injector.getInstance(CmdCacheStores.class));
        register(injector.getInstance(CmdDebug.class));
        register(injector.getInstance(CmdProfile.class));
        register(injector.getInstance(CmdSaveall.class));
        register(injector.getInstance(CmdServer.class));
        register(injector.getInstance(CmdSetID.class));
        register(injector.getInstance(CmdDatabaseList.class));
        register(injector.getInstance(CmdDatabase.class));
        register(injector.getInstance(CmdServers.class));
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("no-permission", "&cNo permission.");
        l.define("player-only", "&cThis command is player-only.");
        l.define("incorrect-usage", "&cIncorrect usage. (needs {0} arguments)  Use: {1} {2}");
        l.define("unknown", "&cUnknown command: '{0}'.  Use /payload for help.");
    }

    @Override
    public String langModule() {
        return "cmd";
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
                    sender.sendMessage(lang.format("player-only"));
                    return true;
                }
                if (!command.permission().has(sender)) {
                    sender.sendMessage(lang.format("no-permission"));
                    return true;
                }
                if (args.length < command.minArgs()) {
                    sender.sendMessage(lang.format("incorrect-usage", command.minArgs() + "", "/" + command.name() + " " + command.usage()));
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
                sender.sendMessage(lang.format("unknown", pCmd));
            }

            return true;
        }
        return false;
    }
}
