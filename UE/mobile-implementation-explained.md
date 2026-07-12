# Mobile Application — Reference Implementation

A line-by-line walkthrough of every class in the Mobile application.
Each snippet is explained in terms of both **what it does** and **why it's designed that way**.

---

## `ITCPClient.java`

```java
public interface ITCPClient {
    void connect(String host, int port) throws IOException;
    void sendFIN();
}
```

| Line | Explanation |
|---|---|
| `interface ITCPClient` | An abstraction — `MobilePhone` depends on this, not on a concrete class. This is **DIP** in action. |
| `connect(String host, int port)` | The only TCP concern a client has — establish a connection to a remote host. |
| `throws IOException` | Declared on the interface so all implementations are forced to handle connection failures honestly. |
| `sendFIN()` | Closes the connection. Named semantically after the TCP FIN signal rather than just `close()` — makes intent clear. No `throws` because it's called from a shutdown hook where checked exceptions can't propagate. |

---

## `IUDPClient.java`

```java
public interface IUDPClient {
    void startStream(String host, int port) throws IOException;
    void stopStream();
}
```

| Line | Explanation |
|---|---|
| `interface IUDPClient` | Segregated from `ITCPClient` — **ISP**. UDP streaming and TCP signalling are different concerns that change for different reasons. |
| `startStream(...)` | Starts continuous audio transmission. Takes host and port so the implementation stays flexible. |
| `stopStream()` | Stops the stream cleanly. No exception declared — stopping should always succeed gracefully. |

---

## `TCPHandler.java`

```java
private Socket socket;
```
Holds the live TCP connection. Private — no other class should touch it directly.

---

```java
public void connect(String host, int port) throws IOException {
    socket = new Socket(host, port);
    System.out.println("[TCP] Connected to MSC at " + host + ":" + port);
}
```

| Line | Explanation |
|---|---|
| `new Socket(host, port)` | Java's standard blocking TCP connect. Throws `IOException` if the MSC isn't reachable — propagated up to `MobilePhone` to handle. |
| `System.out.println(...)` | Simple diagnostic output. In a production system this would be a logger. |

---

```java
public void sendFIN() {
    try {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    } catch (IOException e) {
        System.err.println("[TCP] Error closing connection: " + e.getMessage());
    }
}
```

| Line | Explanation |
|---|---|
| `if (socket != null && !socket.isClosed())` | Defensive guard — `sendFIN()` is called from a shutdown hook which may fire even if `connect()` never succeeded. |
| `socket.close()` | Closing a Java `Socket` sends the TCP FIN signal to the remote end. |
| `catch (IOException e)` | Swallowed here intentionally — we're in teardown, there's nothing meaningful to do with the error. |

---

## `UDPHandler.java`

```java
private volatile boolean streaming = false;
```
`volatile` ensures the flag is visible across threads — the main thread sets it to `false`, the stream thread reads it. Without `volatile` the stream thread might cache a stale `true`.

---

```java
streamThread = new Thread(() -> {
    while (streaming) {
        byte[] audioChunk = captureAudio();
        DatagramPacket packet = new DatagramPacket(audioChunk, audioChunk.length, address, port);
        socket.send(packet);
        Thread.sleep(20);
    }
});
streamThread.setDaemon(true);
streamThread.start();
```

| Line | Explanation |
|---|---|
| `new Thread(() -> {...})` | Audio streaming runs on its own thread — it can't block the main thread. |
| `while (streaming)` | Loop controlled by the `volatile` flag. When `stopStream()` sets it to `false`, the thread exits cleanly on its next iteration. |
| `Thread.sleep(20)` | 20ms between packets simulates 50 packets/second — a realistic audio sampling rate. |
| `setDaemon(true)` | Marks the thread as a daemon so the JVM doesn't wait for it when the application exits. |

---

```java
private byte[] captureAudio() {
    return new byte[160];
}
```
Simulated audio capture returning dummy bytes. In a real implementation this would read from a microphone input stream. The rest of the class doesn't care — **SRP** keeps the simulation detail isolated here.

---

## `CallTimer.java`

```java
public CallTimer(ScreenDisplay display) {
    this.display = display;
}
```
`ScreenDisplay` is injected — `CallTimer` doesn't create it. This is **DIP**: `CallTimer` drives the display but doesn't own it. They can be tested independently.

---

```java
public void start() {
    startTime = LocalDateTime.now();
    running = true;

    tickThread = new Thread(() -> {
        while (running) {
            display.showElapsed(getElapsed());
            Thread.sleep(1000);
        }
    });

    tickThread.setDaemon(true);
    tickThread.start();
}
```

| Line | Explanation |
|---|---|
| `startTime = LocalDateTime.now()` | Captures the call start moment. This becomes part of the CDR later on the MSC side. |
| `new Thread(...)` | The tick runs on its own thread — it fires `showElapsed()` every second without blocking anything else. |
| `display.showElapsed(getElapsed())` | `CallTimer` delegates the display concern to `ScreenDisplay` — each class does one thing. |
| `Thread.sleep(1000)` | One tick per second. |

---

```java
public void stop() {
    running = false;
    endTime = LocalDateTime.now();
}

public Duration getElapsed() {
    LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
    return Duration.between(startTime, end);
}
```

| Line | Explanation |
|---|---|
| `endTime = LocalDateTime.now()` | Captures the exact end moment so `getElapsed()` returns a stable value after the call ends. |
| `(endTime != null) ? endTime : LocalDateTime.now()` | While the call is active `endTime` is null, so we use `now()`. After `stop()`, we use the captured value. |

---

## `ScreenDisplay.java`

```java
public void showElapsed(Duration elapsed) {
    long minutes = elapsed.toMinutes();
    long seconds = elapsed.minusMinutes(minutes).getSeconds();
    System.out.printf("\r[Call] Elapsed: %02d:%02d", minutes, seconds);
}
```

| Line | Explanation |
|---|---|
| `elapsed.toMinutes()` | Extracts the minutes component from the `Duration`. |
| `elapsed.minusMinutes(minutes).getSeconds()` | Subtracts full minutes first to get the remaining seconds — avoids showing `01:70` instead of `01:10`. |
| `\r` | Carriage return without newline — the timer overwrites the same line every second rather than printing a new line. |
| `%02d` | Zero-pads to 2 digits — displays `05` not `5`. |

This class has exactly **one reason to change**: if the display format changes. Nothing else.

---

## `MobilePhone.java` — Orchestrator

```java
public MobilePhone(ITCPClient tcpClient,
                   IUDPClient udpClient,
                   CallTimer callTimer,
                   ScreenDisplay screenDisplay,
                   String msisdn) {
```
Every dependency is injected via constructor. `MobilePhone` never does `new TCPHandler()` internally. This is **DIP** — it depends on abstractions, and makes the class fully testable with mock implementations.

---

```java
public void initiateCall(String destinationMsisdn) {
    tcpClient.connect(MSC_HOST, TCP_PORT);   // 1. Signal the MSC
    udpClient.startStream(MSC_HOST, UDP_PORT); // 2. Start audio
    callTimer.start();                         // 3. Start the timer
}
```
`MobilePhone` coordinates the sequence — it doesn't implement any of these steps itself. This is the **orchestrator pattern**. Each step belongs to a focused collaborator.

---

```java
public void endCall() {
    callTimer.stop();       // 1. Freeze the timer
    udpClient.stopStream(); // 2. Stop sending audio
    tcpClient.sendFIN();    // 3. Signal call end to MSC
}
```
Teardown in reverse concern order — timer first (so elapsed time is captured), then audio, then signalling. `MobilePhone` knows the *order* but not the *implementation*.

---

```java
private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\n[Mobile] Shutdown detected — sending FIN...");
        endCall();
    }));
}
```

| Line | Explanation |
|---|---|
| `addShutdownHook(new Thread(...))` | Registers a thread that the JVM runs automatically when the process is terminated (Ctrl+C, kill signal, etc). |
| `endCall()` | Reuses the same clean teardown path — whether the user ends the call manually or kills the process, the same sequence runs. No duplicated logic. |

---

## `main()` — Wiring Everything Together

```java
public static void main(String[] args) throws InterruptedException {
    ScreenDisplay display = new ScreenDisplay();
    CallTimer timer = new CallTimer(display);
    TCPHandler tcp = new TCPHandler();
    UDPHandler udp = new UDPHandler();

    MobilePhone phone = new MobilePhone(tcp, udp, timer, display, "01012345678");
    phone.initiateCall("01098765432");

    Thread.sleep(10000); // Simulate 10 second call
    phone.endCall();
}
```

| Line | Explanation |
|---|---|
| Manual construction of all dependencies | This is the **composition root** — the one place in the application where concrete classes are instantiated and wired together. `MobilePhone` itself never knows which concrete classes it's using. |
| `new CallTimer(display)` | Notice `display` is created first and passed into `timer` — dependency order matters at wiring time. |
| `Thread.sleep(10000)` | Simulates a 10-second call. In a real app this would block waiting for user input or a network event. |

---

## Full Class Dependency Map

```
main()
  │
  ├── creates ScreenDisplay
  ├── creates CallTimer(display)
  ├── creates TCPHandler
  ├── creates UDPHandler
  └── creates MobilePhone(tcp, udp, timer, display, msisdn)
                │
                ├── ITCPClient.connect()
                ├── IUDPClient.startStream()
                ├── CallTimer.start()
                │       └── ScreenDisplay.showElapsed()
                └── on shutdown:
                    ├── CallTimer.stop()
                    ├── IUDPClient.stopStream()
                    └── ITCPClient.sendFIN()
```

Every arrow points **inward toward abstractions** — no class reaches out to a concrete dependency it didn't receive through its constructor.
