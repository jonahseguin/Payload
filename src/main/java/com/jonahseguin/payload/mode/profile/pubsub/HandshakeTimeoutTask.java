package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import org.bukkit.scheduler.BukkitTask;

public class HandshakeTimeoutTask<X extends PayloadProfile> implements Runnable {

    private final HandshakeManager<X> manager;

    private BukkitTask task = null;

    public HandshakeTimeoutTask(HandshakeManager<X> manager) {
        this.manager = manager;
    }

    public void start() {
        if (this.task == null) {
            this.task = manager.getCache().getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(manager.getCache().getPlugin(), this, 20L, 20L);
        }
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    @Override
    public void run() {
        for (PayloadProfileController<X> controller : this.manager.getCache().getControllers().values()) {
            if (controller.isHandshaking()) {
                // If not init, handshake is complete, or server was found -- don't time them out.
                if (controller.getHandshakeStartTime() > 0 && !controller.isHandshakeComplete() && !controller.isServerFound() && !controller.isAbortHandshakeNotCached()) {
                    long secondsSinceStart = (System.currentTimeMillis() - controller.getHandshakeStartTime()) / 1000;
                    if (secondsSinceStart >= this.manager.getCache().getSettings().getHandshakeTimeoutSeconds()) {
                        controller.setTimedOut(true);
                        // Time out.
                    }
                }
            }
        }
    }
}
