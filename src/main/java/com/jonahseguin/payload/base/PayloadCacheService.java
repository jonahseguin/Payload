package com.jonahseguin.payload.base;

import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.network.NetworkService;
import com.jonahseguin.payload.base.settings.CacheSettings;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.sync.SyncService;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;

public interface PayloadCacheService<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends Service {

    Optional<N> getNetworked(@Nonnull K key);

    Optional<N> getNetworked(@Nonnull X payload);

    Optional<X> get(@Nonnull K key);

    Future<Optional<X>> getAsync(@Nonnull K key);

    Optional<X> getFromCache(@Nonnull K key);

    Optional<X> getFromDatabase(@Nonnull K key);

    boolean save(@Nonnull X payload);

    Future<Boolean> saveAsync(@Nonnull X payload);

    boolean saveNoSync(@Nonnull X payload);

    void cache(@Nonnull X payload);

    void uncache(@Nonnull K key);

    void uncache(@Nonnull X payload);

    void delete(@Nonnull K key);

    void delete(@Nonnull X payload);

    boolean isCached(@Nonnull K key);

    void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<X> callback);

    void prepareUpdateAsync(@Nonnull X payload, @Nonnull PayloadCallback<X> callback);

    void cacheAll();

    @Nullable
    D getData(K key);

    @Nonnull
    Collection<X> getAll();

    @Nonnull
    Collection<X> getCached();

    @Nonnull
    NetworkService<K, X, N, D> getNetworkService();

    @Nonnull
    SyncService<K, X, N, D> getSyncService();

    @Nonnull
    ErrorService getErrorService();

    @Nonnull
    CacheSettings getSettings();

    int saveAll();

    @Nonnull
    PayloadStore<K, X, D> getLocalStore();

    @Nonnull
    PayloadStore<K, X, D> getDatabaseStore();

    @Nonnull
    String getName();

    @Nonnull
    String getServerSpecificName();

    @Nonnull
    Plugin getPlugin();

    @Nonnull
    PayloadMode getMode();

    void setMode(@Nonnull PayloadMode mode);

    void setInstantiator(@Nonnull PayloadInstantiator<K, X, D> instantiator);

    String keyToString(@Nonnull K key);

    K keyFromString(@Nonnull String key);

    void addDepend(@Nonnull PayloadCache cache);

    boolean isDependentOn(@Nonnull PayloadCache cache);

}
