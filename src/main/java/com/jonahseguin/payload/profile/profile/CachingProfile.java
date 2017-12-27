package com.jonahseguin.payload.profile.profile;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.caching.ProfileCachingController;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Getter
@Setter
public class CachingProfile<T extends PayloadProfile> implements ProfilePassable {

    private final PayloadProfileCache<T> cache;
    private final String name;
    private final String uniqueId;
    private final long time;
    private PCacheStage stage = PCacheStage.INIT;
    private PCacheSource loadingSource = null;
    private Player player = null;
    private T temporaryProfile = null;
    private ProfileCachingController<T> controller = null;

    public T getTemporaryProfile() {
        if (temporaryProfile == null) {
            this.temporaryProfile = cache.getLayerController().getCreationLayer().createNewProfile(name, uniqueId);
            this.temporaryProfile.setTemporary(true);
            this.temporaryProfile.setHalted(true);
        }
        return this.temporaryProfile;
    }

    public Player tryToGetPlayer() {
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) {
            return this.player = onlinePlayer;
        }
        return null;
    }


}
