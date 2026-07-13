import java.io.IOException;
import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPHandler implements IUDPClient {

    private DatagramSocket socket;
    private volatile boolean streaming = false;
    private Thread streamThread;
    private TargetDataLine microphone;

    @Override
    public void startStream(String host, int port) throws IOException {
        socket = new DatagramSocket();
        streaming = true;

        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
        } catch (LineUnavailableException e) {
            throw new IOException("Microphone unavailable: " + e.getMessage());
        }

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
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UDP] Audio stream stopped.");
    }

    private byte[] captureAudio() {
        byte[] buffer = new byte[320];
        microphone.read(buffer, 0, buffer.length);
        return buffer;
    }
}
