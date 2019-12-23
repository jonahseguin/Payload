/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.handshake.HandshakeService;
import com.jonahseguin.payload.base.handshake.PayloadHandshakeService;
import com.jonahseguin.payload.database.error.DatabaseErrorService;
import com.jonahseguin.payload.database.internal.InternalPayloadDatabase;
import com.jonahseguin.payload.database.internal.PayloadDatabaseService;
import com.jonahseguin.payload.server.PayloadServerService;
import com.jonahseguin.payload.server.ServerService;

import javax.annotation.Nonnull;

public class DatabaseModule extends AbstractModule {

    private final String name;

    public DatabaseModule(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        this.name = name;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Database.class).toInstance(name);
        bind(ErrorService.class).annotatedWith(Database.class).to(DatabaseErrorService.class);
        bind(DatabaseService.class).to(PayloadDatabaseService.class);
    }

    @Provides
    @Singleton
    PayloadDatabase providePayloadDatabase(PayloadAPI api, Injector injector) {
        if (api.isDatabaseRegistered(name)) {
            return api.getDatabase(name);
        } else {
            PayloadDatabase database = injector.getInstance(InternalPayloadDatabase.class);
            api.registerDatabase(database);
            return database;
        }
    }

    @Provides
    @Singleton
    ServerService provideServerService(PayloadAPI api, Injector injector) {
        if (api.isServerServiceRegistered(name)) {
            return api.getServerService(name);
        } else {
            ServerService service = injector.getInstance(PayloadServerService.class);
            api.registerServerService(service);
            return service;
        }
    }

    @Provides
    @Singleton
    HandshakeService provideHandshakeService(PayloadAPI api, Injector injector) {
        if (api.isHandshakeServiceRegistered(name)) {
            return api.getHandshakeService(name);
        } else {
            HandshakeService service = injector.getInstance(PayloadHandshakeService.class);
            api.registerHandshakeService(service);
            return service;
        }
    }

}
