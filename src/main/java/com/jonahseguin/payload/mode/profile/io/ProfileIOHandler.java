package com.jonahseguin.payload.mode.profile.io;

import org.bukkit.entity.Player;

public interface ProfileIOHandler {

    void loadProfile(String username, String uniqueId, String ip);

    void initialize(Player player);

    void handleQuit(Player player);

}
