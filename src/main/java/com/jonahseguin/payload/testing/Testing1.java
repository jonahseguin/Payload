package com.jonahseguin.payload.testing;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.java.JavaPlugin;

public class Testing1 extends JavaPlugin {

    private PayloadDatabase payloadDatabase = null;
    private ProfileCache<TestProfile> cache = null;

    @Override
    public void onEnable() {
        getLogger().info("Awaiting cache provision from Payload");

        PayloadAPI.get().requestProvision(this).thenAccept(hook -> {
            getLogger().info("Provision received from Payload");

            payloadDatabase = PayloadDatabase.fromConfigFile(this, "database.yml", "purifiedDatabase");
            payloadDatabase.start();

            this.cache = hook.createProfileCache(payloadDatabase,"purifiedProfiles", TestProfile.class);

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
        if (this.payloadDatabase != null) {
            this.payloadDatabase.stop();
        }
    }

}
