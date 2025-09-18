package com.payroc.interviews;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    @Test
    void shouldToggleHealthStatus() {
        Server backendServer = new Server("127.0.0.1", 8080);
        assertTrue(backendServer.isHealthy());
        backendServer.markUnhealthy();
        assertFalse(backendServer.isHealthy());
        backendServer.markHealthy();
        assertTrue(backendServer.isHealthy());
    }

    @Test
    void shouldTrackActiveConnectionCountAccurately() {
        Server backendServer = new Server("127.0.0.1", 8081);
        assertEquals(0, backendServer.getActiveConnections());
        backendServer.incrementActive();
        backendServer.incrementActive();
        assertEquals(2, backendServer.getActiveConnections());
        backendServer.decrementActive();
        backendServer.decrementActive();
        backendServer.decrementActive(); // should not go below zero
        assertEquals(0, backendServer.getActiveConnections());
    }

    @Test
    void toStringShouldIncludeHostAndPort() {
        Server backendServer = new Server("127.0.0.1", 9090);
        String description = backendServer.toString();
        assertTrue(description.contains("127.0.0.1"));
        assertTrue(description.contains("9090"));
    }
}
