package com.example.demo.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Sliding-window limiter kept in memory (the app runs as a single instance). */
public class RateLimiter {
    private final int maxEvents;
    private final long windowNanos;
    private final ConcurrentMap<String, Deque<Long>> events = new ConcurrentHashMap<>();

    public RateLimiter(int maxEvents, Duration window) {
        this.maxEvents = maxEvents;
        this.windowNanos = window.toNanos();
    }

    /** Records an event for {@code key}; returns false when the key already used up its allowance. */
    public boolean tryAcquire(String key) {
        long now = System.nanoTime();
        Deque<Long> queue = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowNanos) queue.pollFirst();
            if (queue.size() >= maxEvents) return false;
            queue.addLast(now);
            return true;
        }
    }

    /** True while {@code key} still has allowance left (does not record anything). */
    public boolean isAllowed(String key) {
        Deque<Long> queue = events.get(key);
        if (queue == null) return true;
        long now = System.nanoTime();
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowNanos) queue.pollFirst();
            return queue.size() < maxEvents;
        }
    }

    public void reset(String key) {
        events.remove(key);
    }
}
