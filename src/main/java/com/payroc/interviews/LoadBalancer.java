package com.payroc.interviews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simplified Layer-4 TCP load balancer.
 * Responsibilities:
 * - Load backend servers from JSON configuration
 * - Accept incoming TCP client connections
 * - Select a backend using a pluggable strategy
 * - Proxy raw bidirectional byte streams
 */
public class LoadBalancer {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final int listenPort;
    private final CopyOnWriteArrayList<Server> backendServers;
    private volatile boolean running = false;
    private ServerSelectionStrategy selectionStrategy;
    private ServerSocket serverSocket;

    public LoadBalancer(int port, String configFilePath, ServerSelectionStrategy selectionStrategy) throws IOException {
        this.listenPort = port;
        this.selectionStrategy = selectionStrategy;
        this.backendServers = new CopyOnWriteArrayList<>(readServers(configFilePath));
    }

    /**
     * Parses backend server definitions from a JSON file.
     *
     * @param path path to JSON file containing an array of {"host":"..","port":number}
     * @return immutable list of parsed servers (empty if file missing or empty)
     * @throws IOException if the file exists but cannot be parsed
     */
    public List<Server> readServers(String path) throws IOException {
        if (path == null) return Collections.emptyList();
        File configFile = new File(path);
        if (!configFile.exists() || !configFile.isFile()) return Collections.emptyList();
        List<Server> parsedServers = JSON_MAPPER.readValue(configFile, new TypeReference<List<Server>>() {
        });
        return parsedServers == null ? Collections.emptyList() : parsedServers;
    }

    public List<Server> getBackends() {
        return List.copyOf(backendServers);
    }

    public void addBackend(Server backendServer) {
        backendServers.addIfAbsent(backendServer);
    }

    public void removeBackend(Server backendServer) {
        backendServers.remove(backendServer);
    }

    public void setStrategy(ServerSelectionStrategy newStrategy) {
        this.selectionStrategy = newStrategy;
    }

    /**
     * Starts the blocking accept loop; spawns a thread per client for proxying.
     */
    public void start() throws IOException {
        if (running) return;
        running = true;
        serverSocket = new ServerSocket(listenPort);
        System.out.println("LB listening on " + listenPort + " with " + backendServers.size() + " backend(s)");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Server selectedServer = selectionStrategy.select(backendServers);
                if (selectedServer == null) {
                    clientSocket.close();
                    continue;
                }
                Thread handlerThread = new Thread(new ConnectionHandler(clientSocket, selectedServer),
                    "conn-" + clientSocket.getPort());
                handlerThread.start();
            } catch (IOException acceptError) {
                if (running) System.err.println("Accept loop error: " + acceptError.getMessage());
            }
        }
    }

    /**
     * Stops accepting new connections and closes the server socket.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException closeError) {
            System.err.println("Error closing accept socket: " + closeError.getMessage());
        }
    }
}
