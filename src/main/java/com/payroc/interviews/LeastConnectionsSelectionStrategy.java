package com.payroc.interviews;

import java.util.List;

/**
 * Selects the server with the fewest active connections.
 */
public class LeastConnectionsSelectionStrategy implements ServerSelectionStrategy {

    @Override
    public Server select(List<Server> servers) {
        if (servers == null || servers.isEmpty()) return null;
        Server best = null;
        for (Server server : servers) {
            if (best == null || server.getActiveConnections() < best.getActiveConnections()) {
                best = server;
            }
        }
        return best;
    }
}
