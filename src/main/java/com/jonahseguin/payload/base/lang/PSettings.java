/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lang;


import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;


@Getter
public class PSettings {

    protected final YamlConfiguration config;
    protected final File file;
    protected final File directory;

    public PSettings(Plugin plugin) {
        this(plugin, "config.yml");
    }

    public PSettings(Plugin plugin, String filename) {
        this(filename, plugin.getDataFolder().getPath());
    }

    public PSettings(String filename, String directory) {
        this.directory = new File(directory);
        this.file = new File(directory, filename);
        config = new YamlConfiguration();
        createFile();
    }

    public void createFile() {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteFile() {
        if (directory.exists()) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}