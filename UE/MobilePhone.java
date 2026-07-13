import java.io.IOException;

public class MobilePhone {

    private final ITCPClient tcpClient;
    private final IUDPClient udpClient;
    private final CallTimer callTimer;
    private final ScreenDisplay screenDisplay;
    private final String msisdn;

    private static final String MSC_HOST = "127.0.0.1";
    private static final int TCP_PORT = 59090;
    private static final int UDP_PORT = 59091;

    public MobilePhone(ITCPClient tcpClient,
                       IUDPClient udpClient,
                       CallTimer callTimer,
                       ScreenDisplay screenDisplay,
                       String msisdn) {
        this.tcpClient = tcpClient;
        this.udpClient = udpClient;
        this.callTimer = callTimer;
        this.screenDisplay = screenDisplay;
        this.msisdn = msisdn;

        registerShutdownHook();
    }

    public void initiateCall(String destinationMsisdn) {
        System.out.println("[Mobile] " + msisdn + " calling " + destinationMsisdn + "...");
        try {
            tcpClient.connect(MSC_HOST, TCP_PORT, msisdn);
            udpClient.startStream(MSC_HOST, UDP_PORT);
            callTimer.start();
            System.out.println("[Mobile] Call established.");
        } catch (IOException e) {
            System.err.println("[Mobile] Call failed: " + e.getMessage());
        }
    }

    public void endCall() {
        System.out.println("\n[Mobile] Ending call...");
        callTimer.stop();
        udpClient.stopStream();
        tcpClient.sendFIN(msisdn);
        System.out.println("[Mobile] Call ended. Duration: " + callTimer.getElapsed().getSeconds() + "s");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Mobile] Shutdown detected — sending FIN...");
            endCall();
        }));
    }

    // --- Entry point ---
    public static void main(String[] args) throws InterruptedException {
        ScreenDisplay display = new ScreenDisplay();
        CallTimer timer = new CallTimer(display);
        TCPHandler tcp = new TCPHandler();
        UDPHandler udp = new UDPHandler();

        MobilePhone phone = new MobilePhone(tcp, udp, timer, display, args[0]);
        phone.initiateCall("01098765432");
    
    }
}
