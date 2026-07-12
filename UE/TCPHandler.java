import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPHandler implements ITCPClient {

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    @Override
    public void connect(String host, int port, String msisdn) throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        System.out.println("[TCP] Connected to MSC at " + host + ":" + port);
        
        // Start background listener to read responses from MSC
        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[MSC]: " + line);
                    if (line.contains("CREDIT_EXHAUSTED_DISCONNECT")) {
                        System.err.println("[Mobile] Credit exhausted! Dropping call...");
                        // Typically we would callback to MobilePhone to end the call here,
                        // but for simulation, we'll just log and let the timer/user handle it,
                        // or we could simulate an immediate exit.
                        System.exit(0);
                    }
                }
            } catch (IOException e) {
                System.err.println("[TCP] Connection read error or closed: " + e.getMessage());
            }
        }).start();

        // Send the START signal to initiate charging
        writer.println("START:" + msisdn);
    }

    @Override
    public void sendFIN(String msisdn) {
        try {
            if (writer != null) {
                writer.println("END:" + msisdn);
                System.out.println("[TCP] Sent END command for " + msisdn);
            }
            if (socket != null && !socket.isClosed()) {
                // Wait briefly to allow END to flush and server to ACK
                Thread.sleep(500); 
                socket.close();
                System.out.println("[TCP] Connection closed.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[TCP] Error closing connection: " + e.getMessage());
        }
    }
}
