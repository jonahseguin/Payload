package com.jonahseguin.payload.testing;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Testing1 extends JavaPlugin {

    private ProfileCache<TestProfile> cache = null;

    @Override
    public void onEnable() {

        PayloadAPI.get().requestProvision(this).thenAccept(hook -> {
            PayloadDatabase db = PayloadDatabase.fromConfigFile(new File(this.getDataFolder() + File.separator + "database.yml"), "purifiedDatabase");

            this.cache = hook.createProfileCache(db,"purifiedProfiles", TestProfile.class);

            if (cache.start()) {
                // success
                getLogger().info("Profile cache started successfully");
            }
            else {
                // fail
            }

        });

    }

    @Override
    public void onDisable() {
        if (this.cache != null) {
            this.cache.stop();
        }
    }

}
