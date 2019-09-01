package com.jonahseguin.payload.base.failsafe;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@Getter
public class FailureManager<K, X extends Payload, D extends PayloadData> implements Runnable {

    private final Map<D, FailedPayload<X, D>> failures = new HashMap<>();
    private final PayloadCache<K, X, D> cache;

    private boolean running = false;
    private BukkitTask task = null;

    public FailureManager(PayloadCache<K, X, D> cache) {
        this.cache = cache;
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
                Player player = PayloadPlugin.get().getServer().getPlayer(profileData.getUniqueId());
                if (player != null) {
                    failedPayload.setPlayer(player);
                    player.sendMessage(cache.getLangController().get(PLang.CACHE_FAILURE_PROFILE_ATTEMPT, cache.getName()));
                } else {
                    purge.add(failedPayload);
                    // Remove them if they aren't online anymore; no point trying to cache them anymore
                }
            }

            // Attempt cache
            X payload = cache.controller(failedPayload.getData()).cache();

            if (payload != null) {
                // If success
                if (failedPayload.getData() instanceof ProfileData) {
                    ProfileData profileData = (ProfileData) failedPayload.getData();
                    Player player = failedPayload.getPlayer();

                    if (player == null || !player.isOnline()) {
                        purge.add(failedPayload);
                        continue;
                    }

                    if (payload instanceof PayloadProfile) {
                        PayloadProfile profile = (PayloadProfile) payload;
                        profile.initializePlayer(player);
                    }
                    player.sendMessage(cache.getLangController().get(PLang.CACHE_FAILURE_PROFILE_ATTEMPT_SUCCESS, cache.getName()));
                }
                purge.add(failedPayload);
            } else {
                // If failure

                if (failedPayload.getData() instanceof ProfileData) {
                    ProfileData profileData = (ProfileData) failedPayload.getData();
                    Player player = PayloadPlugin.get().getServer().getPlayer(profileData.getUniqueId());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(cache.getLangController().get(PLang.CACHE_FAILURE_PROFILE_ATTEMPT_FAILURE, cache.getName(), cache.getSettings().getFailureRetryIntervalSeconds() + ""));
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