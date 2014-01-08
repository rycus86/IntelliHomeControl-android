package hu.rycus.intellihome.network;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An object containing the data to send to the server.
 *
 * Created by Viktor Adam on 10/30/13.
 */
public class Command {

    /** The header of the command. */
    private final int header;
    /** The contents of the command as byte array. */
    private final byte[] data;

    /** The packet received in response to the command. */
    private Packet response;
    /** Lock object to wait for the response. */
    private final Lock responseLock = new ReentrantLock();
    /** Condition object to wait for the response. */
    private final Condition responseCondition = responseLock.newCondition();

    /**
     * Constructor to create a command with no data.
     * @param header The header of the command
     */
    public Command(int header) {
        this.header = header;
        this.data = new byte[0];
    }

    /**
     * Constructor to create a command from data as byte array.
     * @param header The header of the command
     * @param data The contents of the command as byte array
     */
    public Command(int header, byte[] data) {
        this.header = header;
        this.data = data != null ? data : new byte[0];
    }

    /**
     * Constructor to create a command from data as String.
     * @param header The header of the command
     * @param data The contents of the command as String
     */
    public Command(int header, String data) {
        this.header = header;
        this.data = data != null ? data.getBytes() : new byte[0];
    }

    /** Returns the header of the command. */
    public int getHeader() { return header; }

    /** Returns the contents of the command as byte array. */
    public byte[] getData() { return data; }

    /**
     * Returns the contents of the command as String
     * (converted from byte array data using the system's default character set).
     */
    public String getStringData() { return new String(data); }

    /**
     * This method blocks for at most 'timeout' milliseconds
     * then returns the response packet if it was received.
     * @param timeout Timeout in milliseconds
     * @return The received response packet
     */
    public Packet waitForResponse(long timeout) throws InterruptedException {
        responseLock.lock();
        try {
            if(response == null) {
                responseCondition.await(timeout, TimeUnit.MILLISECONDS);
            }
        } finally {
            responseLock.unlock();
        }

        return response;
    }

    /**
     * Sets the received response packet and
     * notifies the waiting thread.
     * @param response The received response packet
     */
    public void setResponse(Packet response) {
        responseLock.lock();
        try {
            this.response = response;
            responseCondition.signal();
        } finally {
            responseLock.unlock();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " H" + Integer.toHexString(getHeader()) + " '" + getStringData() + "'";
    }
}
