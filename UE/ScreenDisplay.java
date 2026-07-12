import java.time.Duration;

public class ScreenDisplay {

    public void showElapsed(Duration elapsed) {
        long minutes = elapsed.toMinutes();
        long seconds = elapsed.minusMinutes(minutes).getSeconds();
        System.out.printf("\r[Call] Elapsed: %02d:%02d", minutes, seconds);
    }
}
