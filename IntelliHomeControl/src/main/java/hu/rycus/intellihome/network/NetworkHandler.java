package hu.rycus.intellihome.network;

import android.util.SparseArray;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Abstract network handler definition.
 *
 * Created by Viktor Adam on 12/4/13.
 */
public abstract class NetworkHandler extends Thread {

    /** The remote manager instance which created this handler. */
    protected final RemoteManager manager;

    /**
     * Set containing header types which should be processed asynchronously.
     * At this level it means that they won't be offered to the packet queue,
     * they will be forwarded to the manager instead to process it directly.
     */
    protected final Set<Integer> asynchHeaders;

    /**
     * Blocking queues containing the received but unprocessed packets for headers.
     */
    protected final SparseArray<LinkedBlockingQueue<Packet>> receivedPackets = new SparseArray<>();

    /** The host of the server. */
    protected final String host;
    /** The port of the server. */
    protected final int port;
    /** The username to authenticate the client. */
    protected final String username;
    /** The password to authenticate the client. */
    protected final String password;

    /** Is this handler still enabled? */
    protected boolean enabled = true;

    /** Is the authenticated user the administrator? */
    protected boolean administrator = false;

    /**
     * Package-private constructor with the creator/owner of the instance.
     * @param name          A name for the thread
     * @param manager       The remote manager instance which created this handler
     * @param host          The host of the server
     * @param port          The port of the server
     * @param username      The username to authenticate the client
     * @param password      The password to authenticate the client
     * @param asynchHeaders Set containing header types which should be processed asynchronously
     */
    protected NetworkHandler(String name, RemoteManager manager,
                             String host, int port, String username, String password,
                             Set<Integer> asynchHeaders) {
        super(name);

        this.manager        = manager;
        this.asynchHeaders  = asynchHeaders;

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /** Initializes this network handler and its connection. */
    protected abstract boolean initialize();

    /** Stops this network handler. */
    protected void shutdown() {
        enabled = false;
        interrupt();
    }

    /** Returns true, if the authenticated user is the administrator. */
    public boolean isAdministrator() { return administrator; }

    /** Executes the main loop of the network handler. */
    public abstract void run();

    /** Polls a queued packet waiting for the given time interval at most to receive one. */
    protected Packet poll(int header, long timeout) {
        try {
            if(timeout > 0L) {
                return getQueue(header).poll(timeout, TimeUnit.MILLISECONDS);
            } else {
                return getQueue(header).poll();
            }
        } catch (InterruptedException e) { }

        return null;
    }

    /** Enqueues a received packet in its header's queue. */
    protected void enqueuePacket(Packet packet) {
        getQueue(packet.getHeader()).offer(packet);
    }

    /** Returns the blocking input queue for the given header value. */
    protected LinkedBlockingQueue<Packet> getQueue(int header) {
        LinkedBlockingQueue<Packet> queue = receivedPackets.get(header);
        if(queue == null) {
            synchronized (receivedPackets) {
                queue = receivedPackets.get(header);
                if(queue == null) {
                    queue = new LinkedBlockingQueue<>();
                    receivedPackets.put(header, queue);
                }
            }
        }
        return queue;
    }

    /**
     * Sends a command with the given header and data contents
     * without prefixing it with the session ID.
     */
    boolean sendWithoutSession(int header, String data) {
        return send(header, data.getBytes(), Flags.WITHOUT_SESSION_ID);
    }
    /**
     * Sends a command with the given header and data contents
     * without prefixing it with the session ID.
     */
    boolean sendWithoutSession(int header, byte[] data) {
        return send(header, data, Flags.WITHOUT_SESSION_ID);
    }
    /** Sends a command with the given header and data contents prefixing it with the session ID. */
    boolean send(int header, String data) {
        return send(header, data.getBytes(), 0);
    }
    /** Sends a command with the given header and no content prefixing it with the session ID. */
    boolean send(int header) {
        return send(header, new byte[0]);
    }
    /** Sends a command with the given header and data contents prefixing it with the session ID. */
    boolean send(int header, byte[] data) {
        return send(header, data, 0);
    }
    /**
     * Sends a command with the given header and data contents
     * modifying it according to the given flags.
     */
    protected abstract boolean send(int header, byte[] data, int flags);

}
