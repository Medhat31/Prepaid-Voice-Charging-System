import java.time.Duration;
import java.time.LocalDateTime;

public class CallTimer {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private volatile boolean running = false;
    private Thread tickThread;
    private final ScreenDisplay display;

    public CallTimer(ScreenDisplay display) {
        this.display = display;
    }

    public void start() {
        startTime = LocalDateTime.now();
        running = true;

        tickThread = new Thread(() -> {
            while (running) {
                try {
                    display.showElapsed(getElapsed());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        tickThread.setDaemon(true);
        tickThread.start();
        System.out.println("[Timer] Call timer started.");
    }

    public void stop() {
        running = false;
        endTime = LocalDateTime.now();
        System.out.println("[Timer] Call timer stopped.");
    }

    public Duration getElapsed() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }
}
