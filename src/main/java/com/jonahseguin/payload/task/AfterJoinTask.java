package com.jonahseguin.payload.task;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachingProfile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class AfterJoinTask {

    private final Map<CachingProfile, Set<JoinTaskRunnable>> tasks = new ConcurrentHashMap<>();
    private final ProfileCache profileCache;
    private BukkitTask bukkitTask = null;

    public AfterJoinTask(ProfileCache profileCache) {
        this.profileCache = profileCache;
    }

    public void startTask() {
        if (bukkitTask == null) {
            this.bukkitTask = profileCache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(
                    profileCache.getPlugin(),
                    () -> {
                        Set<CachingProfile> done = new HashSet<>();
                        for (CachingProfile cachingProfile : tasks.keySet()) {
                            Player player = Bukkit.getPlayer(UUID.fromString(cachingProfile.getUniqueId()));
                            if (player != null && player.isOnline()) {
                                for (JoinTaskRunnable runnable : tasks.get(cachingProfile)) {
                                    runnable.run(cachingProfile, player);
                                }
                                done.add(cachingProfile);
                            }
                        }
                        tasks.keySet().removeAll(done);
                    },
                    20L, 20L
            );
        }
    }

    public void stopTask() {
        if (this.bukkitTask != null) {
            this.bukkitTask.cancel();
            this.bukkitTask = null;
        }
    }

    public void addTask(CachingProfile cachingProfile, JoinTaskRunnable runnable) {
        if (!tasks.containsKey(cachingProfile)) {
            tasks.put(cachingProfile, new HashSet<>());
        }
        tasks.get(cachingProfile).add(runnable);
    }

}
