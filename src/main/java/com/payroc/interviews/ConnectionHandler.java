package com.payroc.interviews;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Proxies a single accepted client TCP connection to a selected backend server.
 * Opens a new outbound socket to the backend and copies bytes in both directions
 * until either side closes or an I/O error occurs.
 */
class ConnectionHandler implements Runnable {
    private final Socket clientSocket;
    private final Server backendServer;

    private static final int BUFFER_SIZE = 8192;
    private static final int JOIN_TIMEOUT_MS = 500;

    ConnectionHandler(Socket clientSocket, Server backendServer) {
        this.clientSocket = clientSocket;
        this.backendServer = backendServer;
    }

    @Override
    public void run() {
        boolean backendConnected = false;
        Socket backendSocket = new Socket();
        try {
            backendSocket.connect(new InetSocketAddress(backendServer.getHost(), backendServer.getPort()));
            backendConnected = true;
            backendServer.incrementActive();

            Thread clientToBackend = new Thread(() -> forward(clientSocket, backendSocket, true), "client->backend");
            clientToBackend.start();

            forward(backendSocket, clientSocket, false);
            try {
                clientToBackend.join(JOIN_TIMEOUT_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException connectErr) {
            System.err.println("ConnectionHandler: backend connect error: " + connectErr.getMessage());
            if (!backendConnected) backendServer.markUnhealthy();
        } finally {
            closeQuietly(clientSocket);
            closeQuietly(backendSocket);
            if (backendConnected) backendServer.decrementActive();
        }
    }

    private void forward(Socket from, Socket to, boolean shutdownOutputAfter) {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
            if (shutdownOutputAfter) {
                try {
                    to.shutdownOutput();
                } catch (IOException ignored) {
                }
            }
        } catch (IOException io) {
            System.err.println("ConnectionHandler forward(" + Thread.currentThread().getName() + "): " + io.getMessage());
        }
    }

    private void closeQuietly(Socket s) {
        if (s == null) return;
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }
}
