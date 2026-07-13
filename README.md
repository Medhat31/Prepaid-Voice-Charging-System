# Prepaid Voice Charging System

A comprehensive, real-time Java-based telecommunications charging simulation. This project emulates the core billing and signaling infrastructure of a Mobile Switching Center (MSC), interacting directly with simulated User Equipment (UE) and an administrative Customer Relationship Management (CRM) web dashboard.

## 🏗️ Architecture & Modules

The system is split into three primary, highly-decoupled modules that integrate seamlessly over TCP/UDP and a shared PostgreSQL database.

### 1. Mobile Switching Center (MSC)
The core charging engine built with Java and Maven. 
- **Signaling Server (TCP)**: Listens on port `59090`. Accepts text-based protocol commands like `START:<msisdn>` and `END:<msisdn>`, and broadcasts real-time events (e.g., `ERROR: MSISDN_NOT_FOUND`, `CREDIT_EXHAUSTED_DISCONNECT`).
- **Media Gateway (UDP)**: Listens on port `59091` to receive simulated voice streams, actively recording the audio data locally during the call.
- **Real-Time Billing**: Utilizes a `ScheduledExecutorService` to concurrently deduct funds from the caller's database balance on a minute-by-minute basis.
- **CDR Generation**: Automatically generates formatted Call Detail Records (CDRs) summarizing the total duration, cost, and termination reason upon call completion.

### 2. User Equipment (UE)
A Java simulator acting as the end-user's mobile phone.
- Automatically connects to the MSC's TCP and UDP channels.
- Executes the protocol handshake, sending the `START` signal and simultaneously establishing the UDP voice stream.
- Operates a background listener thread to intercept network interrupts (like server disconnects or out-of-credit alerts) and handles graceful teardowns.

### 3. CRM (Admin Dashboard)
The administrative Phone Book interface.
- **Backend**: Built with Jakarta RESTful Web Services (JAX-RS) and a pure JDBC repository layer. 
- **Frontend**: A sleek, modern Single Page Application (SPA) built purely with Vanilla HTML, CSS (dark-themed), and JavaScript.
- **Features**: Allows telecom administrators to Create, Read, Update, and Delete subscribers and their balances in the `user_balance` table securely.

### 4. Interactive Voice Response (IVR)
A robust FastAGI server integrated with an Asterisk PBX, providing a real-time bilingual balance checking service over SIP phone lines.
- **Bilingual Routing**: Dynamically switches between English and Arabic utilizing Asterisk's `CHANNEL(language)` variable based on DTMF keypad input.
- **Custom Arabic Grammar Engine**: Features a custom Java algorithm that overrides default Asterisk number parsing to correctly pronounce complex Arabic numbers (21-99) alongside full currency structures.
- **Fault-Tolerant State Machine**: Implements strict nested retry loops, database validation for MSISDNs, and timeout handling, guaranteeing a safe channel `hangup()` in all edge cases.
- **Audio Infrastructure**: Employs strictly formatted 8000Hz Mono GSM audio files for seamless native Asterisk playback.

## 🗄️ Database
The entire suite relies on a unified **PostgreSQL** database (hosted via Neon Database).
- **Table**: `user_balance`
- **Schema**: Maps an auto-incrementing `id`, unique `msisdn`, and precise `NUMERIC` `balance`.
- **Security**: Database credentials (`db.url`, `db.user`, `db.password`) are securely abstracted into `db.properties` files in both the MSC and CRM modules, strictly ignored from version control to prevent credential leaks.

## ⚙️ Tech Stack
* **Language**: Java (JDK 21)
* **Build Tool**: Maven (MSC)
* **Database**: PostgreSQL (JDBC)
* **Networking**: `java.net.Socket`, `java.net.ServerSocket`, `java.net.DatagramSocket`
* **Web APIs**: JAX-RS (Jakarta)
* **Frontend**: HTML5, CSS3, JavaScript (ES6)
* **PBX Engine**: Asterisk, FastAGI (asterisk-java)
* **Audio Processing**: SoX (Sound eXchange)

## 🚀 How to Run

1. **Setup Database**: 
   - Execute the SQL schema on a PostgreSQL instance to generate the `user_balance` table.
   - Create a `db.properties` file inside `MSC/src/main/resources/` and `CRM/` mirroring your DB credentials.
2. **Start the MSC**:
   - `cd MSC`
   - `mvn clean package`
   - `java -jar target/MSC-1.0-SNAPSHOT-jar-with-dependencies.jar`
3. **Launch the UE**:
   - `cd UE`
   - `javac *.java`
   - `java MobilePhone`
4. **Access the CRM**:
   - Compile and deploy the JAX-RS CRM module to your preferred application server (e.g., Tomcat, WildFly) to manage balances via the `index.html` interface.
   
5. **Start the IVR Server & Asterisk:**
1. Compile and run the `IVR.java` FastAGI server.
2. Place your compiled `.gsm` audio files into your server at `/var/lib/asterisk/sounds/en/` and `/var/lib/asterisk/sounds/ar/digits/`.
3. Copy the `extensions.conf` file into `/etc/asterisk/` and execute `dialplan reload` in the Asterisk CLI.
4. Dial the configured extension (e.g., `7000`) from a connected SIP softphone to access the IVR.
