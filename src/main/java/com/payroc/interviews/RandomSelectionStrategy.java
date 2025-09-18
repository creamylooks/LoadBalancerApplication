package com.payroc.interviews;

import java.util.List;
import java.util.Random;

/**
 * Random server selection
 */
public class RandomSelectionStrategy implements ServerSelectionStrategy {
    private final Random rnd = new Random();

    @Override
    public Server select(List<Server> servers) {
        if (servers == null || servers.isEmpty()) return null;
        return servers.get(rnd.nextInt(servers.size()));
    }
}
