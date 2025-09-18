package com.payroc.interviews;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a backend target (host + port).
 */
public class Server {
    private final String host;
    private final int port;
    @JsonIgnore
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    @JsonIgnore
    private final AtomicInteger active = new AtomicInteger(0);

    @JsonCreator
    public Server(@JsonProperty("host") String host,
                  @JsonProperty("port") int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public void markHealthy() {
        healthy.set(true);
    }

    public void markUnhealthy() {
        healthy.set(false);
    }

    public int getActiveConnections() {
        return active.get();
    }

    public void incrementActive() {
        active.incrementAndGet();
    }

    public void decrementActive() {
        active.updateAndGet(v -> v > 0 ? v - 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Server)) return false;
        Server server = (Server) o;
        return port == server.port && Objects.equals(host, server.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port + "(active=" + active.get() + ", healthy=" + healthy.get() + ")";
    }
}
