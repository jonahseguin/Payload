package com.jonahseguin.payload.example;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.java.JavaPlugin;

public class Testing extends JavaPlugin {

    @Override
    public void onEnable() {
        ProfileCache<TestPayload> cache = new ProfileCache<>(this, "myProfileCache");
        cache.start();
    }

    @Override
    public void onDisable() {

    }

    public class TestPayload implements Payload {
        @Override
        public String getIdentifier() {
            return null;
        }

        @Override
        public PayloadCache getCache() {
            return null;
        }
    }

}
