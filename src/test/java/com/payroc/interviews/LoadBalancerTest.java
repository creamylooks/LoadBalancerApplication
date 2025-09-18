package com.payroc.interviews;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {
    private LoadBalancer loadBalancer;

    @AfterEach
    void tearDown() {
        if (loadBalancer != null) loadBalancer.stop();
    }

    @Test
    void shouldLoadBackendServersFromJsonConfiguration() throws Exception {
        int firstBackendPort = findFreePort();
        int secondBackendPort = findFreePort();
        File configFile = createTempConfigFile("[ {\"host\":\"127.0.0.1\",\"port\": " + firstBackendPort + "}, {\"host\":\"127.0.0.1\",\"port\": " + secondBackendPort + "} ]");
        loadBalancer = new LoadBalancer(findFreePort(), configFile.getAbsolutePath(), new RoundRobinSelectionStrategy());
        List<Server> parsedServers = loadBalancer.getBackends();
        assertEquals(2, parsedServers.size());
        assertTrue(parsedServers.stream().anyMatch(server -> server.getPort() == firstBackendPort));
        assertTrue(parsedServers.stream().anyMatch(server -> server.getPort() == secondBackendPort));
    }

    @Test
    void shouldCloseConnectionImmediatelyWhenNoBackendsConfigured() throws Exception {
        File emptyConfigFile = createTempConfigFile("[]");
        loadBalancer = new LoadBalancer(findFreePort(), emptyConfigFile.getAbsolutePath(), new RoundRobinSelectionStrategy());
        Thread loadBalancerThread = new Thread(() -> { try { loadBalancer.start(); } catch (IOException ignored) {} });
        loadBalancerThread.start();
        Thread.sleep(120);
        try (Socket clientSocket = new Socket("127.0.0.1", getLoadBalancerPort(loadBalancer))) {
            // Expect immediate close; absence of hang is success.
        }
        loadBalancer.stop();
        loadBalancerThread.join(500);
    }

    @Test
    void shouldDistributeAndEchoDataAcrossTwoBackends() throws Exception {
        EchoServer firstEchoServer = new EchoServer(findFreePort());
        EchoServer secondEchoServer = new EchoServer(findFreePort());
        firstEchoServer.start();
        secondEchoServer.start();
        File configFile = createTempConfigFile("[ {\"host\":\"127.0.0.1\",\"port\": " + firstEchoServer.getPort() + "}, {\"host\":\"127.0.0.1\",\"port\": " + secondEchoServer.getPort() + "} ]");
        int loadBalancerPort = findFreePort();
        loadBalancer = new LoadBalancer(loadBalancerPort, configFile.getAbsolutePath(), new RoundRobinSelectionStrategy());
        Thread loadBalancerThread = new Thread(() -> { try { loadBalancer.start(); } catch (IOException ignored) {} });
        loadBalancerThread.start();
        Thread.sleep(150);

        for (int attempt = 0; attempt < 6; attempt++) {
            String message = "hello-" + attempt;
            try (Socket clientSocket = new Socket("127.0.0.1", loadBalancerPort)) {
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream clientIn = clientSocket.getInputStream();
                clientOut.write(message.getBytes());
                clientOut.flush();
                clientSocket.shutdownOutput();
                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                byte[] readBuffer = new byte[256];
                int bytesRead;
                while ((bytesRead = clientIn.read(readBuffer)) != -1) responseBuffer.write(readBuffer, 0, bytesRead);
                assertEquals(message, responseBuffer.toString());
            }
        }
        loadBalancer.stop();
        loadBalancerThread.join(500);
        firstEchoServer.stop();
        secondEchoServer.stop();
    }

    // Helper methods
    private int findFreePort() throws IOException { try (ServerSocket probeSocket = new ServerSocket(0)) { return probeSocket.getLocalPort(); } }

    private File createTempConfigFile(String jsonContent) throws IOException {
        File tempFile = Files.createTempFile("lb-config", ".json").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) { writer.write(jsonContent); }
        tempFile.deleteOnExit();
        return tempFile;
    }

    private int getLoadBalancerPort(LoadBalancer loadBalancerInstance) throws Exception {
        var listenPortField = LoadBalancer.class.getDeclaredField("listenPort");
        listenPortField.setAccessible(true);
        return listenPortField.getInt(loadBalancerInstance);
    }

    /** Simple echo server used for integration tests. */
    static class EchoServer {
        private final int port;
        private volatile boolean running = true;
        private ServerSocket serverSocket;
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        EchoServer(int port) { this.port = port; }
        int getPort() { return port; }
        void start() throws IOException {
            serverSocket = new ServerSocket(port);
            executorService.submit(() -> {
                while (running) {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        executorService.submit(() -> handle(acceptedSocket));
                    } catch (IOException acceptError) {
                        if (running) break;
                    }
                }
            });
        }
        private void handle(Socket acceptedSocket) {
            try (Socket socket = acceptedSocket; InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {
                byte[] buffer = new byte[512];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (IOException ignored) {}
        }
        void stop() {
            running = false;
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            executorService.shutdownNow();
        }
    }
}
