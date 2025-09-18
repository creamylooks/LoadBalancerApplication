package com.payroc.interviews;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe round-robin server selection
 */
public class RoundRobinSelectionStrategy implements ServerSelectionStrategy {
    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public Server select(List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        int size = servers.size();
        int idx = Math.abs(cursor.getAndIncrement());
        return servers.get(idx % size);
    }
}
