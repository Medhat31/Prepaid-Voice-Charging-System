
package com.telecom.network;

public interface IUDPServer {
    /**
     * Opens a UDP socket to receive continuous audio packet payloads.
     * @param port The UDP port to bind to (e.g., 9091).
     */
    void startListening(int port);

    /**
     * Halts the UDP data packet processing stream and stops speaker playback.
     */
    void stopListening();
}