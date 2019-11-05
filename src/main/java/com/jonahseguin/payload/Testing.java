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
import com.jonahseguin.payload.mode.profile.ProfileService;
import org.bukkit.plugin.java.JavaPlugin;

public class Testing extends JavaPlugin {

    @Inject
    ProfileService<PayloadProfile> cache;
    @Inject
    DatabaseService database;

    @Override
    public void onEnable() {
        Injector injector = Guice.createInjector(PayloadAPI.install(this, "Database"), new TestingModule(this));
        injector.injectMembers(this);

        if (!database.start()) {
            getLogger().severe("Failed to start database");
            return;
        }

        if (!cache.start()) {
            getLogger().severe("Failed to start profile cache");
            return;
        }

        cache.get("jo19").ifPresent(profile -> profile.msg("Hi!"));
    }

    @Override
    public void onDisable() {
        cache.shutdown();
        database.shutdown();
    }

}
