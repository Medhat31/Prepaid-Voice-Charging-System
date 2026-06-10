
package com.telecom.network;

public interface ITCPServer {
    /**
     * Binds the server socket to a specific port and listens for client signaling.
     * @param port The TCP port to open (e.g., 9090).
     */
    void listen(int port);

    /**
     * Gracefully stops the TCP server loop and terminates client socket channels.
     */
    void stopServer();
}
