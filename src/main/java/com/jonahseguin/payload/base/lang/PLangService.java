/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lang;

import com.jonahseguin.payload.PayloadPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PLangService extends PSettings {

    private final Map<PLang, String> lang = new HashMap<>();

    public PLangService(Plugin plugin) {
        super(plugin, "lang.yml");
        load();

        for (PLang lang : PLang.values()) {
            if (!this.lang.containsKey(lang)) {
                this.lang.put(lang, lang.getLang());
            }
        }

        for (PLang key : lang.keySet()) {
            if (!getConfig().contains(key.name())) {
                getConfig().set(key.name(), lang.get(key));
            }
        }

        for (String key : getConfig().getKeys(false)) {
            PLang lang = PLang.valueOf(key.toUpperCase());
            this.lang.put(lang, getConfig().getString(key));
        }
        save();
    }


    public String format(PLang lang, Object... args) {
        if (!this.lang.containsKey(lang)) {
            this.lang.put(lang, lang.getLang());
        }
        String val = this.lang.get(lang);
        return PayloadPlugin.format(val, args);
    }

}
