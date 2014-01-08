package hu.rycus.intellihome.network;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;

/**
 * TCP/IP based network handler implementation.
 *
 * Created by Viktor Adam on 12/5/13.
 */
public class TCPHandler extends NetworkHandler {

    /** Tag for logcat. */
    private static final String LOG_TAG = "IntelliHome|NET|TCP";

    /** The socket object to connect to the server. */
    private Socket          socket;
    /** The input stream of the socket. */
    private InputStream     input;
    /** The output stream of the socket. */
    private OutputStream    output;

    /** Flag to log connect exceptions. */
    private boolean verboseConnectException = true;

    /**
     * Package-private constructor with the creator/owner of the instance.
     *
     * @see hu.rycus.intellihome.network.NetworkHandler#NetworkHandler(String, RemoteManager, String, int, String, String, java.util.Set)
     */
    protected TCPHandler(RemoteManager manager,
                         String host, int port, String username, String password,
                         Set<Integer> asynchHeaders) {
        super("TCPHandler", manager, host, port, username, password, asynchHeaders);
    }

    @Override
    protected boolean initialize() {
        return true;
    }

    @Override
    protected void shutdown() {
        enabled = false;
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to close TCP socket", ex);
            }
        }
        super.shutdown();
    }

    @Override
    protected boolean send(int header, byte[] data, int flags) {
        if(output == null) {
            Log.e(LOG_TAG, "No output for TCP data");
            return false;
        }

        try {
            output.write(header);

            int length = data.length;
            output.write((length & 0xFF00) >> 8);
            output.write( length & 0x00FF );

            output.write(data);

            return true;
        } catch(Exception ex) {
            Log.e(LOG_TAG, "Failed to send TCP data", ex);
        }

        return false;
    }

    /** Returns true, if the connection is created and login succeeded. */
    private boolean connect() {
        if(socket != null) {
            try {
                socket.close();
            } catch(Exception ex) {
                Log.w(LOG_TAG, "Failed to close previously open socket", ex);
            }
        }

        socket = null;
        input  = null;
        output = null;

        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(10000); // TODO magic number

            verboseConnectException = true;

            input  = socket.getInputStream();
            output = socket.getOutputStream();

            return login();
        } catch (IOException ex) {
            if(verboseConnectException) {
                Log.e(LOG_TAG, "Failed to connect to " + host + ":" + port + " (enabled: " + enabled + ")", ex);
                verboseConnectException = false;
            } else {
                Log.e(LOG_TAG, "Failed to connect to " + host + ":" + port + " (enabled: " + enabled + ") | " + ex);
            }
        }

        return false;
    }

    /** Returns true, if successfully authenticates. */
    private boolean login() {
        administrator = false;

        send(Header.MSG_A_LOGIN, username + ":" + password);

        TcpPacket response = readPacket();
        boolean loggedIn = response != null && response.getHeader() == Header.MSG_A_LOGIN;
        if(loggedIn) {
            administrator = new String(response.getData()).endsWith("*");
        }
        return loggedIn;
    }

    @Override
    public void run() {
        while(enabled) {
            try {
                if(connect()) {

                    while(enabled) {
                        try {
                            TcpPacket tp = readPacket();

                            if(enabled && tp != null) {
                                // signal connection OK
                                manager.setConnected(true);

                                int     header  = tp.getHeader();
                                String  data    = new String(tp.getData());

                                Log.d(LOG_TAG, "TCP Packet received (H" + Integer.toHexString(header) + "), length: " + tp.getLength());

                                if(header == Header.MSG_A_ERROR_INVALID_SESSION) {
                                    Log.e(LOG_TAG, "Invalid session error received");
                                    // signal connection loss
                                    manager.setConnected(false);
                                    break;
                                }

                                Packet packet = new Packet(header, data);
                                if(asynchHeaders.contains(header)) {
                                    manager.processAsynchPacket(packet);
                                } else {
                                    enqueuePacket(packet);
                                }
                            } else {
                                // we are not connected anymore
                                manager.setConnected(false);

                                break;
                            }
                        } catch(Exception ex) {
                            Log.e(LOG_TAG, "Failed to process messages on TCP connection" + " (enabled: " + enabled + ")", ex);
                        }
                    }

                } else {
                    // failed to connect
                    manager.setConnected(false);
                }
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to start connection to " + host + ":" + port + " (enabled: " + enabled + ")", ex);
            }

            if(enabled) {
                synchronized (this) {
                    try {
                        wait(2500); // TODO magic number
                    } catch (InterruptedException e) { /* NO-OP */ }
                }
            }
        }
    }

    /** Reads the next packet from the server. */
    private TcpPacket readPacket() {
        try {
            int header      = input.read();
            int lengthHI    = input.read();
            int lengthLO    = input.read();

            if(header == -1 || lengthHI == -1 || lengthLO == -1) {
                throw new Exception("TCP Socket is closed");
            }

            int length = (lengthHI << 8) | lengthLO;

            byte[] data = new byte[length];

            if(length > 0) {
                int read   = -1;
                int offset =  0;
                while(offset < length && (read = input.read(data, offset, length - offset)) > -1) {
                    offset += read;
                }
            }

            return new TcpPacket(header, data);
        } catch(SocketTimeoutException stex) {
            Log.d(LOG_TAG, "Socket timeout");
        } catch(Exception ex) {
            if(enabled) {
                Log.e(LOG_TAG, "Failed to receive TCP packet" + " (enabled: " + enabled + ")", ex);
            }
        }

        return null;
    }

    /** Data class for incoming packets. */
    private class TcpPacket {

        /** The header of the packet. */
        private final int     header;
        /** The data of the packet. */
        private final byte[]  data;

        /**
         * Private constructor.
         * @param header The header of the packet
         * @param data The data of the packet
         */
        private TcpPacket(int header, byte[] data) {
            this.header = header;
            this.data   = data;
        }

        /** Returns the header of the packet. */
        private int     getHeader() { return header; }
        /** Retruns the data of the packet. */
        private byte[]  getData()   { return data; }
        /** Returns the data length of the packet. */
        private int     getLength() { return data.length; }

    }

}
