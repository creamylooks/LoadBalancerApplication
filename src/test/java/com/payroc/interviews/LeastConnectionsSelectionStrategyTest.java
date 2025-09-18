package com.payroc.interviews;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class LeastConnectionsSelectionStrategyTest {
    @Test
    void shouldSelectBackendWithFewestActiveConnections() {
        Server backendOne = new Server("127.0.0.1", 9201);
        Server backendTwo = new Server("127.0.0.1", 9202);
        Server backendThree = new Server("127.0.0.1", 9203);

        // Simulate differing active counts
        backendOne.incrementActive();
        backendOne.incrementActive(); // backendOne = 2
        backendTwo.incrementActive(); // backendTwo = 1
        // backendThree remains 0 -> should be selected

        LeastConnectionsSelectionStrategy strategy = new LeastConnectionsSelectionStrategy();
        assertEquals(backendThree, strategy.select(List.of(backendOne, backendTwo, backendThree)));
    }

    @Test
    void shouldReturnNullWhenNoBackendsConfigured() {
        LeastConnectionsSelectionStrategy strategy = new LeastConnectionsSelectionStrategy();
        assertNull(strategy.select(List.of()));
    }
}
