package com.payroc.interviews;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class RandomSelectionStrategyTest {
    @Test
    void returnsNullOnEmpty() {
        RandomSelectionStrategy randomStrategy = new RandomSelectionStrategy();
        assertNull(randomStrategy.select(List.of()));
    }

    @Test
    void returnsEachConfiguredBackendOverMultipleSelections() {
        RandomSelectionStrategy randomStrategy = new RandomSelectionStrategy();
        List<Server> backendServers = List.of(
            new Server("127.0.0.1", 9101),
            new Server("127.0.0.1", 9102)
        );
        boolean observedFirstPort = false;
        boolean observedSecondPort = false;
        for (int attempt = 0; attempt < 200; attempt++) { // 200 to reduce unlikely bias
            Server chosen = randomStrategy.select(backendServers);
            assertNotNull(chosen);
            if (chosen.getPort() == 9101) observedFirstPort = true;
            if (chosen.getPort() == 9102) observedSecondPort = true;
            if (observedFirstPort && observedSecondPort) break;
        }
        assertTrue(observedFirstPort && observedSecondPort, "Both backend ports should be observed over multiple random selections");
    }
}
