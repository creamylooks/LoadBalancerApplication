package com.payroc.interviews;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionHandlerTest {
    private ServerSocket backendEchoServerSocket;
    private ServerSocket loadBalancerListenerSocket;

    @AfterEach
    void cleanup() throws Exception {
        if (backendEchoServerSocket != null) backendEchoServerSocket.close();
        if (loadBalancerListenerSocket != null) loadBalancerListenerSocket.close();
    }

    @Test
    void shouldProxyDataBidirectionallyBetweenClientAndBackend() throws Exception {
        int backendPort = findFreePort();
        backendEchoServerSocket = new ServerSocket(backendPort);
        CountDownLatch backendAcceptedLatch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().submit(() -> {
            try (Socket backendSideSocket = backendEchoServerSocket.accept()) {
                backendAcceptedLatch.countDown();
                byte[] buffer = new byte[1024];
                InputStream backendInput = backendSideSocket.getInputStream();
                OutputStream backendOutput = backendSideSocket.getOutputStream();
                int bytesRead;
                while ((bytesRead = backendInput.read(buffer)) != -1) {
                    backendOutput.write(buffer, 0, bytesRead);
                    backendOutput.flush();
                }
            } catch (Exception e) {
                System.err.println("Backend echo server error: " + e.getMessage());
            }
        });

        int loadBalancerPort = findFreePort();
        loadBalancerListenerSocket = new ServerSocket(loadBalancerPort);
        CountDownLatch clientAcceptedLatch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Socket acceptedClientSocket = loadBalancerListenerSocket.accept();
                clientAcceptedLatch.countDown();
                Server backendDefinition = new Server("127.0.0.1", backendPort);
                backendDefinition.incrementActive();
                Thread handlerThread = new Thread(new ConnectionHandler(acceptedClientSocket, backendDefinition));
                handlerThread.start();
                handlerThread.join(2000);
            } catch (Exception e) {
                System.err.println("Load balancer listener error: " + e.getMessage());
            }
        });

        try (Socket clientSocket = new Socket("127.0.0.1", loadBalancerPort)) {
            assertTrue(clientAcceptedLatch.await(500, TimeUnit.MILLISECONDS));
            assertTrue(backendAcceptedLatch.await(500, TimeUnit.MILLISECONDS));
            OutputStream clientOutput = clientSocket.getOutputStream();
            InputStream clientInput = clientSocket.getInputStream();
            String payload = "ping-test";
            clientOutput.write(payload.getBytes());
            clientOutput.flush();
            clientSocket.shutdownOutput();
            byte[] responseBytes = clientInput.readAllBytes();
            assertEquals(payload, new String(responseBytes));
        }
    }

    private int findFreePort() throws Exception {
        try (ServerSocket probeSocket = new ServerSocket(0)) {
            return probeSocket.getLocalPort();
        }
    }
}
