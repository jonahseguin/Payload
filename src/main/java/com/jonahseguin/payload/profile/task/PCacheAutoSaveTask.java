package com.jonahseguin.payload.profile.task;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.Profile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Created by Jonah on 10/21/2017.
 * Project: purifiedCore
 *
 * @ 1:53 PM
 */
@Getter
public class PCacheAutoSaveTask<T extends Profile> implements Runnable {

    private final PayloadProfileCache<T> cache;
    private BukkitTask task;

    public PCacheAutoSaveTask(PayloadProfileCache<T> cache) {
        this.cache = cache;
        // Will auto-save all online profile every 15 minutes
        this.task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this,
                (15 * 60 * 20), (15 * 60 * 20));
    }

    @Override
    public void run() {
        int success = 0;
        int error = 0;
        StringBuilder failed = new StringBuilder();
        for (Player pl : Bukkit.getOnlinePlayers()) {
            T profile = cache.getLocalProfile(pl);
            if (profile != null) {
                boolean redis = cache.getLayerController().getRedisLayer().save(profile);
                boolean mongo = cache.getLayerController().getMongoLayer().save(profile);
                if (redis && mongo) {
                    success++;
                } else {
                    error++;
                    failed.append(pl.getName()).append(" ");
                }
            } else {
                error++;
                failed.append(pl.getName()).append(" ");
            }
        }
        cache.getDebugger().debug(ChatColor.GRAY + "Auto-save complete.  " + success + " obj saved successfully, " + error + " players failed to save.");
        if (error > 0) {
            cache.getDebugger().debug(ChatColor.RED + "The following players failed to save: " + failed.toString());
        }
    }
}
