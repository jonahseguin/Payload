package com.jonahseguin.payload.profile.task;

import com.jonahseguin.payload.profile.profile.CachingProfile;

import org.bukkit.entity.Player;

/**
 * Created by Jonah on 10/20/2017.
 * Project: purifiedCore
 *
 * @ 4:44 PM
 */
public interface PJoinTask {

    void run(CachingProfile cachingProfile, Player player);

}
