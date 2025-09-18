package com.payroc.interviews;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoundRobinSelectionStrategyTest {

    @Test
    void shouldSelectServersInRoundRobinOrder() {
        RoundRobinSelectionStrategy roundRobinStrategy = new RoundRobinSelectionStrategy();
        List<Server> serverList = List.of(
            new Server("127.0.0.1", 8001),
            new Server("127.0.0.1", 8002),
            new Server("127.0.0.1", 8003)
        );

        Server first = roundRobinStrategy.select(serverList);
        Server second = roundRobinStrategy.select(serverList);
        Server third = roundRobinStrategy.select(serverList);
        Server fourth = roundRobinStrategy.select(serverList); // cycles back

        assertEquals(8001, first.getPort());
        assertEquals(8002, second.getPort());
        assertEquals(8003, third.getPort());
        assertEquals(8001, fourth.getPort());
        assertEquals("127.0.0.1", first.getHost());
        assertEquals("127.0.0.1", second.getHost());
        assertEquals("127.0.0.1", third.getHost());
        assertEquals("127.0.0.1", fourth.getHost());
    }

    @Test
    void shouldReturnNullWhenServerListIsEmpty() {
        RoundRobinSelectionStrategy roundRobinStrategy = new RoundRobinSelectionStrategy();
        assertNull(roundRobinStrategy.select(List.of()));
    }
}
