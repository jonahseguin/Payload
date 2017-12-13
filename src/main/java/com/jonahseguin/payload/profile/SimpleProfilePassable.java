package com.jonahseguin.payload.profile;

import lombok.Data;
import org.bukkit.entity.Player;

@Data
public class SimpleProfilePassable implements ProfilePassable {

    private final String uniqueId;
    private final String name;

    public static SimpleProfilePassable fromPlayer(Player player) {
        return new SimpleProfilePassable(player.getUniqueId().toString(), player.getName());
    }

}
