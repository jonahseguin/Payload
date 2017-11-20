package com.jonahseguin.payload.task;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Created by Jonah on 10/21/2017.
 * Project: purifiedCore
 *
 * @ 1:53 PM
 */
@Getter
public class CacheAutoSaveTask implements Runnable {

    private final ProfileCache cache;
    private BukkitTask task;

    public CacheAutoSaveTask(ProfileCache cache) {
        this.cache = cache;
        // Will auto-save all online profiles every 15 minutes
        this.task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this,
                (15 * 60 * 20), (15 * 60 * 20));
    }

    @Override
    public void run() {
        int success = 0;
        int error = 0;
        String failed = "";
        for (Player pl : Bukkit.getOnlinePlayers()) {
            Profile profile = cache.getLocalProfile(pl);
            if (profile != null) {
                boolean redis = cache.getRedisLayer().save(profile);
                boolean mongo = cache.getMongoLayer().save(profile);
                if (redis && mongo) {
                    success++;
                } else {
                    error++;
                    failed += pl.getName() + " ";
                }
            } else {
                error++;
                failed += pl.getName() + " ";
            }
        }
    }
}
