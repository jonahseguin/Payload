package com.jonahseguin.payload.simple.type;

import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import org.bukkit.entity.Player;

public interface SimpleInstantiator<X extends PlayerCacheable> {

    X instantiate(Player player);

}
