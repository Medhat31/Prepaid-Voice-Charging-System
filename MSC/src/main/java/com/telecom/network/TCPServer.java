package com.telecom.network;

import com.telecom.app.IMSC;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ITCPServer {

    private final IMSC msc;
    private volatile boolean isRunning;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public TCPServer(IMSC msc) {
        this.msc = msc;
    }

    @Override
    public void listen(int port) {
        this.isRunning = true;

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            
            System.out.println("Attempting binding sequence on port " + port + "...");
            serverSocket.bind(new InetSocketAddress(port));
            
            System.out.println("TCP Signaling channel online on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getRemoteSocketAddress());

                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Server socket failure: " + e.getMessage());
            }
        } finally {
          
            if (serverSocket != null && serverSocket.isBound()) {
                stopServer();
            } else {
                this.isRunning = false;
                System.err.println("[TCP ENGINE]: Initialization aborted safely. Port remains held by system.");
            }
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("CONNECTED: Welcome to the Telecom Server.");
            String inputLine;

            while ((inputLine = reader.readLine()) != null) {
                inputLine = inputLine.trim();
                System.out.println("[SIGNAL]: " + inputLine);

                if (inputLine.startsWith("START:")) {
                    String msisdn = inputLine.substring(6).trim();
                    msc.onCallStart(msisdn, writer);


                } else if (inputLine.startsWith("END:")) {
                    String msisdn = inputLine.substring(4).trim();
                    msc.onCallEnd(msisdn);
                    writer.println("ACK: END " + msisdn);

                } else if (inputLine.equalsIgnoreCase("QUIT") || inputLine.equalsIgnoreCase("EXIT")) {
                    writer.println("BYE");
                    break;
                } else {
                    writer.println("ERROR: Use START:<msisdn> or END:<msisdn>");
                }
            }
        } catch (IOException e) {
            System.err.println("Client closed suddenly: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Connection closed cleanly.");
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void stopServer() {

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error forcing TCP server socket closure: " + e.getMessage());
        }

        if (!isRunning) {
            return;
        }
        this.isRunning = false;

        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
        System.out.println("TCP Engine offline.");
    }
}
