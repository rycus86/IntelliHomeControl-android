package hu.rycus.intellihome.network;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * UDP based network handler implementation.
 *
 * Created by Viktor Adam on 10/30/13.
 */
class UDPHandler extends NetworkHandler {

    /** Tag for logcat. */
    private static final String LOG_TAG = "IntelliHome|NET|UDP";

    /** The buffer size used in communication. */
    private static final int bufferSize = 1500;

    /** The UDP/Multicast socket used for communication. */
    private MulticastSocket socket;

    /** The session ID used in communication (as sent by the remote server). */
    private String sessionID = null;

    /** The last known address of the remote server. */
    private SocketAddress address;

    /** Map containing incomplete multipart packets by header. */
    private final Map<Integer, Packet> incompleteMessages = new HashMap<>();

    /** Is the discovery multicast based? */
    private final boolean multicast;
    /** Is the discovery broadcast based? */
    private final boolean broadcast;

    /**
     * Package-private constructor.
     *
     * @param multicast Pass true, if the discovery is multicast based
     * @param broadcast Pass true, if the discovery is broadcast based
     *
     * @see hu.rycus.intellihome.network.NetworkHandler#NetworkHandler(String, RemoteManager, String, int, String, String, java.util.Set)
     */
    UDPHandler(RemoteManager manager,
               String host, int port, String username, String password,
               Set<Integer> asynchHeaders,
               boolean multicast, boolean broadcast) {
        super("UDPHandler", manager, host, port, username, password, asynchHeaders);

        this.multicast = multicast;
        this.broadcast = broadcast;
    }

    /**
     * Initializes the connection by setting the address, port number and other settings
     * of the multicast UDP socket, then joins the multicast group.
     */
    @Override
    protected boolean initialize() {
        try {
            socket = new MulticastSocket(port);
            socket.setTimeToLive(8); // TODO magic numbers
            socket.setSoTimeout(20000);
            socket.setLoopbackMode(true);
        } catch(Exception ex) {
            Log.e(LOG_TAG, "Failed to start UDP socket on port " + port, ex);
        }

        if(socket != null) {
            address = new InetSocketAddress(host, port);

            if(multicast) {
                try {
                    socket.joinGroup(InetAddress.getByName(host));
                    return true;
                } catch(Exception ex) {
                    Log.e(LOG_TAG, "Failed to join multicast group at " + host, ex);
                }
            } else if(broadcast) {
                try {
                    socket.setBroadcast(true);
                    return true;
                } catch(Exception ex) {
                    Log.e(LOG_TAG, "Failed to set Broadcast socket flag", ex);
                }
            } else {
                // unicast UDP socket initialized
                return true;
            }
        }

        return false;
    }

    /** Stops this network handler instance. */
    @Override
    protected void shutdown() {
        super.shutdown();

        if(socket != null) socket.close();

        Log.e(LOG_TAG, "Network handler stopped");
    }

    /**
     * <p>
     *     This basically waits for an UDP packet,
     *     merges it with a previous one if it was multipart,
     *     then either sends it to the remote manager to process it
     *     or queues it to have it processed elsewhere.
     * </p>
     * <p>
     *     This code automatically handles session parameter changes
     *     and reconnections in case of communication loss.
     * </p>
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        boolean skipNextLogin = false;

        while (enabled) {
            if(sessionID == null && !skipNextLogin) {
                sendWithoutSession(Header.MSG_A_LOGIN, username + ":" + password); // login to server
            }

            skipNextLogin = false;

            // wait for a packet
            DatagramPacket dp = null;

            try {
                dp = receivePacket();
            } catch(BroadcastLoopbackReceivedException blrex) {
                skipNextLogin = true;
                continue; // drop our own broadcast packet
            }

            if(enabled && dp != null && dp.getLength() >= 2) {
                // signal connection OK
                manager.setConnected(true);

                byte[] buffer = dp.getData();

                int header  = buffer[0] & 0xFF;
                int flags   = buffer[1] & 0xFF;
                String data = new String(buffer, 2, dp.getLength() - 2);

                boolean finish = (flags & Flags.MORE_FOLLOWS) != Flags.MORE_FOLLOWS;
                Log.d(LOG_TAG,
                        "UDP Packet received (H" + Integer.toHexString(header) + "), " +
                                "length: " + (dp.getLength() - 2) + " | " + (finish ? "Complete" : "INCOMPLETE"));

                if(finish && header == Header.MSG_A_LOGIN) {
                    // set session parameters
                    address = dp.getSocketAddress();
                    sessionID = data;
                    if(data.endsWith("*")) {
                        administrator = true;
                        sessionID = sessionID.substring(0, sessionID.length() - 1);
                    } else {
                        administrator = false;
                    }

                    Log.i(LOG_TAG, "Login result: " + sessionID + " source: " + address);

                    continue;
                } else if(finish && header == Header.MSG_A_ERROR_INVALID_SESSION) {
                    Log.e(LOG_TAG, "Invalid session error received");
                    // signal connection loss
                    manager.setConnected(false);
                    sessionID = null;

                    if(enabled) {
                        // wait some then retry

                        synchronized (this) {
                            try {
                                wait(2500);
                            } catch (InterruptedException e) { }
                        }

                        continue;
                    }
                }

                // create a new version of a packet, possibly by merging this to a previous one
                Packet packet = mergeIncomplete(header, data, finish);
                if(packet != null) {
                    // process this packet
                    if(asynchHeaders.contains(header)) {
                        manager.processAsynchPacket(packet);
                    } else {
                        enqueuePacket(packet);
                    }
                }
            } else {
                // we are not connected anymore
                manager.setConnected(false);
            }
        }
    }

    /**
     * Creates a new version of a packet, possibly by merging the given data into a previous packet.
     * @param header The header of the received packet
     * @param data   The (possibly partial) data of a packet
     * @param finish Is this the last packet? (or else more follows)
     * @return A new version of a packet with the given header
     */
    private Packet mergeIncomplete(int header, String data, boolean finish) {
        Packet incomplete = incompleteMessages.remove(header);

        // concatenate data
        String newData = incomplete != null ? (incomplete.getData() + data) : data;
        Packet packet = new Packet(header, newData);

        if(finish) {
            return packet;
        } else {
            incompleteMessages.put(header, packet);
            return null;
        }
    }

    /** Helper method to receive one UDP datagram packet. */
    private DatagramPacket receivePacket() {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
            socket.receive(packet);

            if(broadcast) {
                try {
                    NetworkInterface nif = NetworkInterface.getByInetAddress(packet.getAddress());
                    if(nif != null) throw new BroadcastLoopbackReceivedException(); // drop our own packet
                } catch(SocketException sex) {
                    Log.w(LOG_TAG, "Failed to determine if a broadcast packet was ours", sex);
                    throw new BroadcastLoopbackReceivedException();
                }
            }

            return packet;
        } catch(SocketTimeoutException stex) {
            Log.d(LOG_TAG, "Socket timeout");
        } catch(Exception ex) {
            if(ex instanceof BroadcastLoopbackReceivedException) {
                throw (BroadcastLoopbackReceivedException) ex; // allow throwing this type exception
            }

            if(enabled) {
                Log.e(LOG_TAG, "Failed to receive datagram packet", ex);
            }
        }

        return null;
    }

    /** @see hu.rycus.intellihome.network.NetworkHandler#send(int, byte[], int)  */
    @Override
    protected boolean send(int header, byte[] data, int flags) {
        Log.v(LOG_TAG, "Sending H" + Integer.toHexString(header) + ": " + new String(data));

        if( (flags & Flags.WITHOUT_SESSION_ID) != Flags.WITHOUT_SESSION_ID ) {
            if(sessionID == null) {
                return false; // can not send it withoutg session ID
            }

            byte[] fulldata = new byte[sessionID.length() + data.length];

            System.arraycopy(sessionID.getBytes(), 0, fulldata, 0, sessionID.length());
            System.arraycopy(data, 0, fulldata, sessionID.length(), data.length);

            data = fulldata;
        }

        int maxSize = bufferSize - 2; // BufferSize - (HeaderLength + FlagsLength)
        int toSend = data.length;

        int offset = 0;

        // send multipart chunks
        while (toSend > maxSize) {
            flags |= Flags.MORE_FOLLOWS;

            byte[] buffer = new byte[bufferSize];
            buffer[0] = (byte) header;
            buffer[1] = (byte) flags;
            System.arraycopy(data, offset, buffer, 2, maxSize);

            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
                socket.send(packet);

                Log.v(LOG_TAG, "Sent " + buffer.length + " bytes");
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to send message to " + address, ex);
                return false;
            }

            offset += maxSize;
            toSend -= maxSize;
        }

        flags &= ~Flags.MORE_FOLLOWS;

        // send the rest of the message
        if (toSend > 0 || data.length == 0) {
            byte[] buffer = new byte[2 + data.length];
            buffer[0] = (byte) header;
            buffer[1] = (byte) flags;
            if(data.length > 0) {
                System.arraycopy(data, offset, buffer, 2, data.length);
            }

            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
                socket.send(packet);

                Log.v(LOG_TAG, "Sent " + buffer.length + " bytes");
            } catch(Exception ex) {
                Log.e(LOG_TAG, "Failed to send message to " + address, ex);
                return false;
            }
        }

        return true;
    }

    /** Exception when our own message was received on loopback. */
    private class BroadcastLoopbackReceivedException extends RuntimeException { }

}
