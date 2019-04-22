package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.Bukkit;

public class DefaultErrorHandler<K, X extends Payload> implements PayloadErrorHandler<K, X> {

    @Override
    public void exception(Throwable throwable) {
        Bukkit.getLogger().warning("Payload error: " + throwable.getMessage());
    }

    @Override
    public void error(String message) {
        Bukkit.getLogger().warning("Payload error: " + message);
    }

    @Override
    public void error(PayloadCache cache, String message) {
        Bukkit.getLogger().warning("Payload error: " + message);
    }

    @Override
    public void exception(PayloadCache cache, Throwable throwable) {
        Bukkit.getLogger().warning("Payload error: " + throwable.getMessage());
    }

    @Override
    public void exception(Throwable throwable, String message) {
        Bukkit.getLogger().warning("Payload error: " + message + " :" + throwable.getMessage());
    }

    @Override
    public void exception(PayloadCache cache, Throwable throwable, String message) {
        Bukkit.getLogger().warning("Payload error: " + message + " :" + throwable.getMessage());
    }
}
