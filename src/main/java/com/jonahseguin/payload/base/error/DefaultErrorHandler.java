package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.PayloadPlugin;

public class DefaultErrorHandler implements PayloadErrorHandler {

    @Override
    public void debug(String cacheName, String message) {
        PayloadPlugin.get().getLogger().info("[Debug][" + cacheName + "] " + message);
    }

    @Override
    public void error(String cacheName, String message) {
        PayloadPlugin.get().getLogger().info("[Error][" + cacheName + "] " + message);
    }

    @Override
    public void exception(String cacheName, Throwable throwable) {
        PayloadPlugin.get().getLogger().info("[Exception][" + cacheName + "] " + throwable.getMessage());
        if (this.isDebug()) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void exception(String cacheName, Throwable throwable, String message) {
        PayloadPlugin.get().getLogger().info("[Exception][" + cacheName + "] " + message);
        if (this.isDebug()) {
            throwable.printStackTrace();
        }
    }

    public boolean isDebug() {
        return PayloadPlugin.get().isDebug();
    }

}
