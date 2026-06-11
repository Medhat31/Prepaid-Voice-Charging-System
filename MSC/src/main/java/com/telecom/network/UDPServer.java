package com.telecom.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UDPServer implements IUDPServer {

    private DatagramSocket udpSocket;
    private volatile boolean isRunning;

    @Override
    public void startListening(int port) {
        this.isRunning = true;

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(null);
                udpSocket.setReuseAddress(true);
                udpSocket.bind(new InetSocketAddress(port));
                byte[] buffer = new byte[1024];
                System.out.println("UDP voice channel online on port " + port);

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    int bytesReceived = packet.getLength();
                    System.out.println("Received " + bytesReceived + " bytes of voice stream.");
                }
            } catch (SocketException e) {
                if (isRunning) {
                    System.err.println("Socket setup failed: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error reading data packet: " + e.getMessage());
            } finally {
                stopListening();
            }
        }, "UDP-Voice-Stream-Thread").start();
    }

    @Override
    public synchronized void stopListening() {
        this.isRunning = false;

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            System.out.println("UDP Voice Channel offline.");
        }
    }

}
