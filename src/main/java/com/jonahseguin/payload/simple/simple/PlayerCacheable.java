package com.jonahseguin.payload.simple.simple;

import org.bukkit.entity.Player;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 5:40 PM
 */
public interface PlayerCacheable {

    Player getPlayer();

    void initialize(Player player);

}
