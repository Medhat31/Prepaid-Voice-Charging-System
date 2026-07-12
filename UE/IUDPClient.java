import java.io.IOException;

public interface IUDPClient {
    void startStream(String host, int port) throws IOException;
    void stopStream();
}
