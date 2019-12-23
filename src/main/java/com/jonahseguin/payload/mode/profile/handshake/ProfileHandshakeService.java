/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.handshake;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.server.PayloadServer;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ProfileHandshakeService<X extends PayloadProfile> implements Service {

    private final ProfileCache<X> cache;
    private final DatabaseService database;
    private final String channelRequest;
    private final String channelReply;
    private boolean running = false;
    private RedisPubSubReactiveCommands<String, String> reactive = null;

    @Inject
    public ProfileHandshakeService(@Nonnull ProfileCache<X> cache, @Nonnull DatabaseService database) {
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        Preconditions.checkNotNull(database, "Database cannot be null");
        Preconditions.checkNotNull(cache.getName(), "Cache name cannot be null");
        this.cache = cache;
        this.database = database;
        this.channelRequest = "payload-profile-handshake-" + cache.getName() + "-request";
        this.channelReply = "payload-profile-handshake-" + cache.getName() + "-reply";
    }

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "Profile Handshake Service is already running!");
        boolean sub = subscribe();
        running = true;
        return sub;
    }

    @Override
    public boolean shutdown() {
        Preconditions.checkState(running, "Profile Handshake Service is not running!");
        if (reactive != null) {
            reactive.unsubscribe(channelRequest, channelReply);
        }
        running = false;
        return false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private boolean subscribe() {
        try {
            StatefulRedisPubSubConnection<String, String> connection = database.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe(channelRequest, channelReply).subscribe();

            reactive.observeChannels()
                    .filter(pm -> pm.getChannel().equals(channelRequest) || pm.getChannel().equals(channelReply))
                    .doOnNext(patternMessage -> {
                        ProfileHandshakePacket packet = ProfileHandshakePacket.fromJSON(patternMessage.getMessage());
                        if (packet != null) {
                            if (packet.getTargetServer().equalsIgnoreCase(database.getServerService().getThisServer().getName())) {
                                if (patternMessage.getChannel().equals(channelRequest)) {
                                    handleRequest(packet);
                                } else if (patternMessage.getChannel().equals(channelRequest)) {
                                    handleReply(packet);
                                }
                            }
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error subscribing in Profile Handshake Service for channels: " + channelRequest + ", " + channelReply);
            return false;
        }
    }

    private void sendReply(@Nonnull ProfileHandshakePacket requestPacket) {
        Preconditions.checkNotNull(requestPacket, "RequestPacket cannot be null in ProfileHandshakeService (in sendReply)");
        requestPacket.setTargetServer(requestPacket.getSenderServer());
        requestPacket.setSenderServer(database.getServerService().getThisServer().getName());
        String json = requestPacket.toDocument().toJson();
        Preconditions.checkNotNull(json, "JSON cannot be null for sendReply in ProfileHandshakeService");
        database.getRedisPubSub().async().publish(channelReply, json);
    }

    private void handleRequest(@Nonnull final ProfileHandshakePacket packet) {
        Preconditions.checkNotNull(packet, "ProfileHandshakePacket cannot be null");
        // Another server is being joined by the player (who is assumingly on this server)
        // - check if they're online, if they are: save them
        // - after save (or if they're not online) send reply
        final Player player = cache.getPlugin().getServer().getPlayer(packet.getUuid());
        cache.runAsync(() -> {
            if (player != null && player.isOnline()) {
                cache.getFromCache(player).ifPresent(cache::save);
            }
            sendReply(packet);
        });
    }

    private void handleReply(@Nonnull ProfileHandshakePacket packet) {
        Preconditions.checkNotNull(packet, "ProfileHandshakePacket cannot be null");
        UUID uuid = packet.getUuid();
        if (uuid != null) {
            PayloadProfileController<X> controller = cache.controller(uuid);
            controller.setHandshakeComplete(true);
            controller.setHandshakeTimedOut(false);
        }
    }

    public void handshake(@Nonnull PayloadProfileController<X> controller, PayloadServer targetServer) {
        ProfileHandshakePacket packet = new ProfileHandshakePacket(database.getServerService().getThisServer().getName(), controller.getUuid(), targetServer.getName());
        String json = packet.toDocument().toJson();
        Preconditions.checkNotNull(json, "JSON cannot be null for handshake in ProfileHandshakeService");
        controller.setHandshakeTimedOut(false);
        controller.setHandshakeComplete(false);
        controller.setHandshakeRequestStartTime(System.currentTimeMillis());
        database.getRedisPubSub().async().publish(channelRequest, json);
    }

}
