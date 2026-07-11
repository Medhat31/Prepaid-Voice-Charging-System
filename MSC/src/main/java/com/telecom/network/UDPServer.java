package com.telecom.network;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import javax.sound.sampled.*;

public class UDPServer implements IUDPServer {

    private DatagramSocket udpSocket;
    private volatile boolean isRunning;
    private SourceDataLine speaker;
    private RandomAccessFile recordStream;
    private int recordedBytes = 0;

    @Override
    public void startListening(int port) {
        this.isRunning = true;

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(null);
                udpSocket.setReuseAddress(true);
                udpSocket.bind(new InetSocketAddress(port));
                
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
                try {
                    speaker = AudioSystem.getSourceDataLine(format);
                    speaker.open(format);
                    speaker.start();
                } catch (Exception e) {
                    System.err.println("Speaker unavailable for live playback: " + e.getMessage());
                    speaker = null;
                }
                
                byte[] buffer = new byte[1024];
                System.out.println("UDP voice channel online on port " + port);

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    
                    int length = packet.getLength();
                    if (length > 0) {
                        // Play audio
                        if (speaker != null) {
                            speaker.write(buffer, 0, length);
                        }
                        // Record audio
                        synchronized (this) {
                            if (recordStream != null) {
                                recordStream.write(buffer, 0, length);
                                recordedBytes += length;
                            }
                        }
                    }
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
        
        if (speaker != null) {
            speaker.drain();
            speaker.stop();
            speaker.close();
            speaker = null;
        }
        
        stopRecording();
    }

    @Override
    public synchronized void startRecording(String callId) {
        if (recordStream != null) {
            stopRecording();
        }
        try {
            File recordDir = new File("/tmp/charging/records");
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }
            String recordFileName = "/tmp/charging/records/call_" + callId + "_" + System.currentTimeMillis() + ".wav";
            recordStream = new RandomAccessFile(recordFileName, "rw");
            recordedBytes = 0;
            recordStream.write(new byte[44]);
            System.out.println("Recording audio to: " + recordFileName);
        } catch (IOException e) {
            System.err.println("Failed to start recording: " + e.getMessage());
            recordStream = null;
        }
    }

    @Override
    public synchronized void stopRecording() {
        if (recordStream != null) {
            try {
                recordStream.seek(0);
                recordStream.writeBytes("RIFF");
                recordStream.writeInt(Integer.reverseBytes(36 + recordedBytes));
                recordStream.writeBytes("WAVE");
                recordStream.writeBytes("fmt ");
                recordStream.writeInt(Integer.reverseBytes(16));
                recordStream.writeShort(Short.reverseBytes((short) 1));
                recordStream.writeShort(Short.reverseBytes((short) 1));
                recordStream.writeInt(Integer.reverseBytes(8000));
                recordStream.writeInt(Integer.reverseBytes(16000));
                recordStream.writeShort(Short.reverseBytes((short) 2));
                recordStream.writeShort(Short.reverseBytes((short) 16));
                recordStream.writeBytes("data");
                recordStream.writeInt(Integer.reverseBytes(recordedBytes));
                
                recordStream.close();
            } catch (IOException e) {
                System.err.println("Failed to finalize record stream: " + e.getMessage());
            } finally {
                recordStream = null;
            }
        }
    }

}
