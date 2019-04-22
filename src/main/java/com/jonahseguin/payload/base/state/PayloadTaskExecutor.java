package com.jonahseguin.payload.base.state;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PayloadTaskExecutor<K, X extends Payload> {

    private final PayloadCache<K, X> cache;
    private final BlockingQueue<PayloadTask> tasks = new LinkedBlockingQueue<>();
    private volatile boolean running = false;
    private volatile boolean allowSubmission = false;

    public PayloadTaskExecutor(PayloadCache<K, X> cache) {
        this.cache = cache;
    }

    public void start() {
        if (!running) {
            this.cache.getPool().submit(() -> {
                while(this.running) {
                    if (this.cache.isLocked()) continue;
                    try {
                        this.tasks.take().run().run();
                    }
                    catch (InterruptedException ex) {
                        cache.getErrorHandler().exception(cache, ex, "Interruption in Payload Task Executor");
                    }
                }
            });
            this.allowSubmission = true;
        }
        else {
            throw new IllegalStateException("Payload Task Executor is already running; cannot start");
        }
    }

    public void stop() {
        if (running) {
            this.allowSubmission = false;
            this.running = false; // Will cause the loop in the thread to stop, and thus the thread to end and be returned
        }
        else {
            throw new IllegalStateException("Payload Task Executor is not running; cannot stop");
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean canSubmit() {
        return this.allowSubmission;
    }

    public boolean submit(PayloadTask runnable) {
        if (this.canSubmit()) {
            return this.tasks.add(runnable);
        }
        else {
            throw new IllegalStateException("Task submission is currently not allowed, due to shutdown or startup in progress");
        }
    }

}
