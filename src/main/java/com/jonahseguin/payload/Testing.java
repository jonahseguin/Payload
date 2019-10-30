/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.java.JavaPlugin;

public class Testing extends JavaPlugin {

    @Inject
    ProfileCache<PayloadProfile> profileCache;
    @Inject
    DatabaseService database;

    @Override
    public void onEnable() {
        Injector injector = Guice.createInjector(PayloadAPI.install(this), new TestingModule(this));
        injector.injectMembers(this);
        database.load("database.yml", "Database");
        if (!database.start()) {
            getLogger().severe("Failed to start database");
            return;
        }

        if (!profileCache.start()) {
            getLogger().severe("Failed to start profile cache");
            return;
        }

        PayloadProfile jo19 = profileCache.getProfileByName("jo19");
        jo19.msg("Hey!");
    }

    @Override
    public void onDisable() {

    }

}
