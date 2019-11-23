/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.database.DatabaseService;
import org.bson.Document;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

public class PayloadHandshakeService implements HandshakeService {

    private final String name;
    private final ConcurrentMap<String, Handshake> replyControllers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HandshakeContainer> containers = new ConcurrentHashMap<>();
    private boolean running = false;
    private final DatabaseService database;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    public PayloadHandshakeService(DatabaseService database) {
        this.database = database;
        this.name = database.getName();
    }

    @Override
    public boolean start() {
        running = true;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean shutdown() {
        this.containers.values().stream()
                .map(HandshakeContainer::getSubscriberController)
                .forEach(Handshake::stopListening);
        shutdownExecutor();
        this.containers.clear();
        this.replyControllers.clear();
        running = false;
        return true;
    }

    private void shutdownExecutor() {
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            database.getErrorService().capture(ex, "Interrupted during shutdown of handshake service's executor service");
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public void receiveReply(@Nonnull String channel, @Nonnull HandshakeData data) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(data);
        if (replyControllers.containsKey(data.getID())) {
            Handshake controller = replyControllers.get(data.getID());
            if (controller != null) {
                executor.submit(() -> {
                    controller.load(data);
                    controller.executeHandler();
                });
            }
            replyControllers.remove(data.getID());
        }
    }

    @Override
    public void receive(@Nonnull String channel, @Nonnull HandshakeData data) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(data);
        // Receiving before sending reply
        if (containers.containsKey(channel)) {
            HandshakeContainer container = containers.get(channel);
            Handshake controller = container.createInstance();
            executor.submit(() -> {
                controller.load(data);
                if (controller.shouldAccept()) {
                    controller.receive();
                    if (controller.shouldReply()) {
                        try (Jedis jedis = database.getJedisResource()) {
                            jedis.publish(controller.channelReply(), data.getDocument().toJson());
                        } catch (Exception ex) {
                            database.getErrorService().capture(ex, "Error with Jedis resource during handshake receive (sending reply) for " + controller.getClass().getSimpleName());
                        }
                    }
                }
            });
        }
    }

    @Override
    public <H extends Handshake> void subscribe(@Nonnull H subscriber) {
        Preconditions.checkNotNull(subscriber);
        HandshakeContainer container = new HandshakeContainer(subscriber);
        Handshake controller = container.getSubscriberController();
        containers.put(controller.channelPublish(), container);
        containers.put(controller.channelReply(), container);
        executor.submit(controller::listen);
    }

    @Override
    public <H extends Handshake> HandshakeHandler<H> publish(@Nonnull H controller) {
        Preconditions.checkNotNull(controller);
        HandshakeData data = new HandshakeData(new Document());
        data.writeID();
        controller.write(data);
        HandshakeHandler<H> handler = new HandshakeHandler<>(data);
        controller.setHandler(handler);
        replyControllers.put(data.getID(), controller);
        try (Jedis jedis = database.getJedisResource()) {
            jedis.publish(controller.channelPublish(), data.getDocument().toJson());
        } catch (Exception ex) {
            database.getErrorService().capture(ex, "Error with Jedis resource during handshake publish for " + controller.getClass().getSimpleName());
        }
        return handler;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }
}
