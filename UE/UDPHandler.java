import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPHandler implements IUDPClient {

    private DatagramSocket socket;
    private volatile boolean streaming = false;
    private Thread streamThread;

    @Override
    public void startStream(String host, int port) throws IOException {
        socket = new DatagramSocket();
        streaming = true;

        InetAddress address = InetAddress.getByName(host);

        streamThread = new Thread(() -> {
            while (streaming) {
                try {
                    byte[] audioChunk = captureAudio();
                    DatagramPacket packet = new DatagramPacket(audioChunk, audioChunk.length, address, port);
                    socket.send(packet);
                    Thread.sleep(20);
                } catch (IOException | InterruptedException e) {
                    if (streaming) {
                        System.err.println("[UDP] Stream error: " + e.getMessage());
                    }
                }
            }
        });

        streamThread.setDaemon(true);
        streamThread.start();
        System.out.println("[UDP] Audio stream started to " + host + ":" + port);
    }

    @Override
    public void stopStream() {
        streaming = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UDP] Audio stream stopped.");
    }

    private byte[] captureAudio() {
        // Simulated audio capture — returns dummy bytes
        return new byte[160];
    }
}
