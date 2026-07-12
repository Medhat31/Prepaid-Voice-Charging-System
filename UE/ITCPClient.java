import java.io.IOException;

public interface ITCPClient {
    void connect(String host, int port, String msisdn) throws IOException;
    void sendFIN(String msisdn);
}
