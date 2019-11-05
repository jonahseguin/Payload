/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.failsafe;

import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadController;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@Getter
public class FailureManager<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> implements Runnable, LangModule {

    private final PayloadPlugin payloadPlugin;
    private final Map<D, FailedPayload<X, D>> failures = new HashMap<>();
    private final PayloadCache<K, X, N, D> cache;

    private boolean running = false;
    private BukkitTask task = null;

    public FailureManager(PayloadCache<K, X, N, D> cache, PayloadPlugin payloadPlugin) {
        this.cache = cache;
        this.payloadPlugin = payloadPlugin;
        cache.getLang().register(this);
    }

    public void start() {
        if (!this.running) {
            task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this, 20L, 20L);
            this.running = true;
        }
    }

    public void stop() {
        if (this.running) {
            this.task.cancel();
            this.task = null;
            this.running = false;
        }
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("cache-attempt-failed", "&7[{0}] &cFailed to load your profile.  Trying again in {1} seconds.");
        l.define("cache-attempt-success", "&7[{0}] &aLoaded your profile successfully.");
    }

    @Override
    public String langModule() {
        return "failure";
    }

    public void fail(D data) {
        if (this.hasFailure(data)) return;
        FailedPayload<X, D> failedPayload = new FailedPayload<>(data, System.currentTimeMillis());
        this.failures.put(data, failedPayload);
    }

    public boolean hasFailure(D data) {
        return this.failures.containsKey(data);
    }

    public boolean hasFailure(UUID uuid) {
        return this.failures.values().stream().anyMatch(fp -> {
            if (fp.getData() instanceof ProfileData) {
                ProfileData data = (ProfileData) fp.getData();
                return data.getUniqueId().equals(uuid);
            }
            return false;
        });
    }

    public FailedPayload<X, D> getFailedPayload(UUID uuid) {
        return this.failures.values().stream().filter(fp -> {
            if (fp.getData() instanceof ProfileData) {
                ProfileData data = (ProfileData) fp.getData();
                return data.getUniqueId().equals(uuid);
            }
            return false;
        }).findFirst().orElse(null);
    }

    public FailedPayload<X, D> getFailedPayload(D data) {
        return this.failures.get(data);
    }

    @Override
    public void run() {
        Set<FailedPayload<X, D>> purge = new HashSet<>();

        for (FailedPayload<X, D> failedPayload : this.failures.values()) {
            if (failedPayload.getLastAttempt() > 0) {
                long secondsSinceLastAttempt = (System.currentTimeMillis() - failedPayload.getLastAttempt()) / 1000;
                if (secondsSinceLastAttempt < this.cache.getSettings().getFailureRetryIntervalSeconds()) {
                    continue;
                }
            }
            // Update last attempt
            failedPayload.setLastAttempt(System.currentTimeMillis());

            // Special case for ProfileData
            if (failedPayload.getData() instanceof ProfileData) {
                ProfileData profileData = (ProfileData) failedPayload.getData();
                Player player = payloadPlugin.getServer().getPlayer(profileData.getUniqueId());
                if (player != null) {
                    failedPayload.setPlayer(player);
                    player.sendMessage(cache.getLang().module(this).format("cache-attempt-failed", cache.getName(), cache.getSettings().getFailureRetryIntervalSeconds()));
                } else {
                    purge.add(failedPayload);
                    // Remove them if they aren't online anymore; no point trying to cache them anymore
                }
            }

            // Attempt cache
            PayloadController<X> controller = cache.controller(failedPayload.getData());
            Optional<X> o = controller.cache();

            if (o.isPresent()) {
                X payload = o.get();
                // If success
                if (failedPayload.getData() instanceof ProfileData) {
                    ProfileData profileData = (ProfileData) failedPayload.getData();
                    Player player = failedPayload.getPlayer();

                    if (player == null || !player.isOnline()) {
                        purge.add(failedPayload);
                        continue;
                    }

                    if (controller instanceof PayloadProfileController) {
                        PayloadProfileController profileController = (PayloadProfileController) controller;
                        profileController.initializeOnJoin(player);
                    }
                    player.sendMessage(cache.getLang().module(this).format("cache-attempt-success", cache.getName()));
                }
                cache.cache(payload);
                purge.add(failedPayload);
            } else {
                // If failure

                if (failedPayload.getData() instanceof ProfileData) {
                    ProfileData profileData = (ProfileData) failedPayload.getData();
                    Player player = payloadPlugin.getServer().getPlayer(profileData.getUniqueId());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(cache.getLang().module(this).format("cache-attempt-failed", cache.getName(), cache.getSettings().getFailureRetryIntervalSeconds()));
                    } else {
                        purge.add(failedPayload);
                    }
                }
            }
        }

        // Remove failures in the purge (either a player logged out or we successfully cached)
        purge.forEach(f -> this.failures.remove(f.getData()));
    }

}
