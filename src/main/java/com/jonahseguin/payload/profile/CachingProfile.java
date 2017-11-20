package com.jonahseguin.payload.profile;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Getter
@Setter
public class CachingProfile<T extends Profile> implements ProfilePassable {

    private final ProfileCache<T> cache;
    private final String name;
    private final String uniqueId;
    private final long time;
    private CacheStage stage = CacheStage.INIT;
    private CacheSource loadingSource = null;
    private Player player = null;
    private T temporaryProfile = null;
    private CachingController<T> controller = null;

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
