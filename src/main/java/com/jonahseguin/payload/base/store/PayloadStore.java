package com.jonahseguin.payload.base.store;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface PayloadStore<K, X extends Payload, D extends PayloadData> extends Service {

    Optional<X> get(@Nonnull K key);

    Optional<X> get(@Nonnull D data);

    boolean save(@Nonnull X payload);

    boolean has(@Nonnull K key);

    boolean has(@Nonnull D data);

    boolean has(@Nonnull X payload);

    void remove(@Nonnull K key);

    void remove(@Nonnull D data);

    void remove(@Nonnull X payload);

    @Nonnull
    Collection<X> getAll();

    int cleanup();

    long clear();

    @Nonnull
    String layerName();

    long size();

    boolean isDatabase();

}
