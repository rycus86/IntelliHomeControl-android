package hu.rycus.intellihome.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;
import hu.rycus.intellihome.model.Entity;
import hu.rycus.intellihome.model.EntityHistory;
import hu.rycus.intellihome.model.EntityType;
import hu.rycus.intellihome.model.User;
import hu.rycus.intellihome.util.Defaults;
import hu.rycus.intellihome.util.Intents;
import hu.rycus.intellihome.util.PreferenceKeys;

/**
 * Manager object responsible for controlling the remote server and handling responses from it.
 *
 * Created by Viktor Adam on 10/30/13.
 */
public class RemoteManager extends Thread {

    /** The current instance of the manager. */
    private static RemoteManager INSTANCE = null;

    /** Tag for logcat. */
    private static final String LOG_TAG = "IntelliHome|RM";

    /** The remote service that created this manager. */
    private final RemoteService remoteService;

    /** The network handler used for low-level communication with the server. */
    private NetworkHandler handler;

    /** Queue for commands to send to the server. */
    private final LinkedBlockingQueue<Command> queue = new LinkedBlockingQueue<>();

    /** Executor for various background tasks (like downloading posters). */
    private final ExecutorService executor;

    /** True until this manager instance is enabled. */
    private boolean enabled = true;
    /** True if this manager is connected and has an active session to the server. */
    private boolean connected = false;

    /**
     * Private constructor initiating and starting the manager.
     * @param service The remote service that created this manager
     */
    private RemoteManager(RemoteService service) {
        super("RemoteManager");
        this.remoteService = service;
        this.executor = Executors.newSingleThreadExecutor();
        this.start();
    }

    /**
     * Starts a manager instance (stopping the previous one if there was one.
     * @param service The remote service that creates a manager
     * @return The started manager instance
     */
    public static RemoteManager start(RemoteService service) {
        synchronized (RemoteManager.class) {
            if(INSTANCE != null) {
                INSTANCE.shutdown();
            }

            INSTANCE = new RemoteManager(service);
            Log.e(LOG_TAG, "RM.Instance: CREATE");
            return INSTANCE;
        }
    }

    /** Creates a network handler according to user preferences. */
    protected NetworkHandler createNetworkHandler() {
        Set<Integer> asynchHeaders = new HashSet<>(Arrays.asList(
                Header.MSG_A_ERROR,
                Header.MSG_A_KEEPALIVE,
                Header.MSG_A_USERS_CHANGED,
                Header.MSG_A_SEND_COMMAND,
                Header.MSG_A_STATE_CHANGED
        ));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(remoteService);
        String  mode    = prefs.getString(PreferenceKeys.Communication.MODE,   Defaults.Communication.MODE);
        String  strPort = prefs.getString(PreferenceKeys.Communication.PORT,   Defaults.Communication.PORT);
        int     port    = Integer.parseInt(strPort);

        String  username = prefs.getString(PreferenceKeys.Authentication.USERNAME, Defaults.Authentication.USERNAME);
        String  password = prefs.getString(PreferenceKeys.Authentication.PASSWORD, Defaults.Authentication.PASSWORD);

        switch (mode) {
            case "mcast":
            {
                String group = prefs.getString(PreferenceKeys.Communication.HOST, Defaults.Communication.MCAST_GROUP);
                return new UDPHandler(this, group, port, username, password, asynchHeaders, true, false);
            }
            case "bcast":
            {
                String address = prefs.getString(PreferenceKeys.Communication.HOST, Defaults.Communication.MCAST_GROUP);
                return new UDPHandler(this, address, port, username, password, asynchHeaders, false, true);
            }
            case "udp":
            {
                String host = prefs.getString(PreferenceKeys.Communication.HOST, null);
                if (host == null) {
                    throw new RuntimeException("No address available for UDP/unicast communication"); // This shouldn't happen
                }
                return new UDPHandler(this, host, port, username, password, asynchHeaders, false, false);
            }
            case "tcp":
            {
                String host = prefs.getString(PreferenceKeys.Communication.HOST, null);
                if (host == null) {
                    throw new RuntimeException("No address available for TCP communication"); // This shouldn't happen
                }
                return new TCPHandler(this, host, port, username, password, asynchHeaders);
            }
        }

        return null;
    }

    /**
     * This code instantiates a low-level network handler, logs in then sends commands
     * to the remote server or a keep-alive message if no commands were enqueued.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        handler = createNetworkHandler();
        if(handler == null) {
            throw new RuntimeException("Couldn't create the network handler"); // This shouldn't happen
        }

        try {
            if( handler.initialize() ) {
                Log.e(LOG_TAG, "Network handler initialized");
                handler.start();

                while(enabled && (INSTANCE == this)) {
                    try {
                        // TODO magic numbers
                        Command command = queue.poll(connected ? 7500 : 2500, TimeUnit.MILLISECONDS);
                        if(command != null) {
                            process(command);
                        } else {
                            handler.send(Header.MSG_A_KEEPALIVE);
                        }
                    } catch(Exception ex) {
                        Log.e(LOG_TAG, "Failed to process a command", ex);
                    }
                }
            }
        } finally {
            handler.shutdown();
            setConnected(false);
        }

        if(INSTANCE != this) {
            Log.e(LOG_TAG, "Remote manager differs from CURRENT_INSTANCE");
        }

        Log.e(LOG_TAG, "Remote manager stopped");
    }

    /** Stops this remote manager instance. */
    public void shutdown() {
        synchronized (RemoteManager.class) {
            connected = false;
            enabled = false;
            executor.shutdown();
            queue.offer(new Command(Header.MSG_A_EXIT));

            if(INSTANCE == this) {
                INSTANCE = null;
                Log.e(LOG_TAG, "RM.Instance: null");
            }
        }
    }

    /** Returns true if this manager is connected and has an active session to the server. */
    public boolean isConnected() { return connected; }
    /** Set this true if this manager is connected and has an active session to the server. */
    void setConnected(boolean connected) {
        if(this.connected != connected) {
            Intent intent = new Intent(Intents.ACTION_CALLBACK);
            intent.putExtra(Intents.EXTRA_CONNECTION_STATE, connected);
            LocalBroadcastManager.getInstance(remoteService).sendBroadcast(intent);
        }

        this.connected = connected;
    }

    /** Returns true, if the authenticated user is the administrator. */
    public boolean isAdministratorUser() {
        return handler.isAdministrator();
    }

    /**
     * Processes an enqueued command.
     * This usually means sending the command to the remote server,
     * processing the response and notifying the UI and/or the user.
     */
    private void process(Command command) {
        switch (command.getHeader()) {
            case Header.MSG_A_EXIT:
            {
                logoff();
                break;
            }
            case Header.MSG_A_LIST_DEVICE_TYPES:
            {
                listDeviceTypes();
                break;
            }
            case Header.MSG_A_LIST_DEVICES:
            {
                listDevices(command.getStringData());
                break;
            }
            case Header.MSG_A_SEND_COMMAND:
            {
                doSendCommand(command.getHeader(), command.getStringData());
                break;
            }
            case Header.MSG_A_RENAME_DEVICE:
            {
                handler.send(command.getHeader(), command.getStringData());
                break;
            }
            case Header.MSG_A_COUNT_HISTORY:
            case Header.MSG_A_LIST_HISTORY:
            {
                handler.send(command.getHeader(), command.getStringData());

                Packet response = handler.poll(command.getHeader(), 5000);
                command.setResponse(response);

                break;
            }
            case Header.MSG_A_LIST_USERS:
            {
                listUsers();
                break;
            }
            case Header.MSG_A_USER_CREATE:
            case Header.MSG_A_USER_EDIT:
            case Header.MSG_A_USER_DELETE:
            {
                handler.send(command.getHeader(), command.getStringData());
                break;
            }
            default:
                Log.e(LOG_TAG, "Unprocessed command received with header: 0x" + Integer.toHexString(command.getHeader()));
                break;
        }
    }

    /** Sends an exit message to the remote server. */
    private void logoff() {
        handler.send(Header.MSG_A_EXIT);
    }

    /* Implementations of protocol messages. */

    public void requestDeviceTypeList() {
        queue.offer(new Command(Header.MSG_A_LIST_DEVICE_TYPES));
    }

    private void listDeviceTypes() {
        handler.send(Header.MSG_A_LIST_DEVICE_TYPES);

        BroadcastHelper helper = new BroadcastHelper(Intents.ACTION_DEVICE_TYPES_LISTED);

        try {
            Packet response = handler.poll(Header.MSG_A_LIST_DEVICE_TYPES, 5000);
            if(response != null) {
                String data = response.getData();
                if(data.matches("\\[.*\\]")) {
                    data = data.substring(1, data.length() - 1);

                    int offset = 0;
                    while(offset < data.length()) {
                        int re = EntityType.deserialize(data, offset);
                        offset += re;
                    }
                } else {
                    helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_invalid_response));
                }
            } else {
                helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_list_device_types));
            }
        } catch(Exception ex) {
            helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_list_device_types) + " | " + ex);
        }

        helper.send(remoteService);

        for(final EntityType et : EntityType.list()) {
            if(et.getImageFilename() != null) {
                executor.execute(new Runnable() {
                    @Override public void run() {
                        loadImage(et);
                    }
                });
            }
        }
    }

    public void requestDeviceList(Integer type) {
        String parameter = type != null ? Integer.toString(type) : null;
        queue.offer(new Command(Header.MSG_A_LIST_DEVICES, parameter));
    }

    private void listDevices(String parameters) {
        handler.send(Header.MSG_A_LIST_DEVICES, parameters);

        BroadcastHelper helper = new BroadcastHelper(Intents.ACTION_DEVICE_LIST);

        try {
            Packet response = handler.poll(Header.MSG_A_LIST_DEVICES, 5000);
            if(response != null) {
                String data = response.getData();
                if(data.matches("\\[.*\\]")) {
                    data = data.substring(1, data.length() - 1);

                    List<Entity> entities = new LinkedList<>();
                    Entity[] eOuput = new Entity[1];

                    int offset = 0;
                    while(offset < data.length()) {
                        int re = Entity.deserialize(data, offset, eOuput);
                        entities.add(eOuput[0]);
                        offset += re;
                    }

                    helper.addParameter(Intents.EXTRA_DEVICE_LIST_ENTITIES, entities.toArray(new Entity[0]));
                } else {
                    helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_invalid_response));
                }
            } else {
                helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_list_devices));
            }
        } catch(Exception ex) {
            helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_list_devices));
        }

        helper.send(remoteService);
    }

    public void sendCommand(String entityId, int commandId, String parameter) {
        String command = parameter != null ? (commandId + ";" + parameter) : Integer.toString(commandId);
        queue.offer(new Command(Header.MSG_A_SEND_COMMAND, entityId + "#" + command));
    }

    private void doSendCommand(int header, String command) {
        handler.send(header, command);
    }

    public void renameDevice(String entityId, String name) {
        queue.offer(new Command(Header.MSG_A_RENAME_DEVICE, entityId + ";" + name));
    }

    public int countHistory(Long tsFrom, Long tsTo, String entityId) {
        StringBuilder parameterBuilder = new StringBuilder();
        if(tsFrom != null) parameterBuilder.append(tsFrom);
        parameterBuilder.append(";");
        if(tsTo != null) parameterBuilder.append(tsTo);
        parameterBuilder.append(";");
        if(entityId != null) parameterBuilder.append(entityId);

        Command command = new Command(Header.MSG_A_COUNT_HISTORY, parameterBuilder.toString());

        queue.offer(command);
        Packet packet = null;

        try {
            packet = command.waitForResponse(7500);
        } catch(InterruptedException iex) { /* NO-OP */ }

        if(packet != null) {
            return Integer.parseInt(packet.getData());
        }

        return -1;
    }

    public EntityHistory[] listHistory(Long tsFrom, Long tsTo, String entityId, int limit, int offset) {
        StringBuilder parameterBuilder = new StringBuilder();
        if(tsFrom != null) parameterBuilder.append(tsFrom);
        parameterBuilder.append(";");
        if(tsTo != null) parameterBuilder.append(tsTo);
        parameterBuilder.append(";");
        if(entityId != null) parameterBuilder.append(entityId);
        parameterBuilder.append(";");
        parameterBuilder.append(limit);
        parameterBuilder.append(";");
        parameterBuilder.append(offset);

        Command command = new Command(Header.MSG_A_LIST_HISTORY, parameterBuilder.toString());

        queue.offer(command);
        Packet packet = null;

        try {
            packet = command.waitForResponse(7500);
        } catch(InterruptedException iex) { /* NO-OP */ }

        if(packet != null) {
            String data = packet.getData();

            ArrayList<EntityHistory> items = new ArrayList<>(limit);
            EntityHistory[] ehOutput = new EntityHistory[1];

            int start = 0;
            while(start + 1 < data.length()) {
                if(data.charAt(start) == '#') {
                    start += EntityHistory.deserialize(data, start + 1, ehOutput);
                    items.add(ehOutput[0]);
                }
            }

            return items.toArray(new EntityHistory[0]);
        }

        return null;
    }

    public void requestUserList() {
        queue.offer(new Command(Header.MSG_A_LIST_USERS));
    }

    private void listUsers() {
        handler.send(Header.MSG_A_LIST_USERS);

        BroadcastHelper helper = new BroadcastHelper(Intents.ACTION_USER_LIST);

        Packet packet = handler.poll(Header.MSG_A_LIST_USERS, 5000);
        if(packet != null) {
            String data = packet.getData();
            String[] dataArray = data.split(";");

            User[] users = new User[dataArray.length];

            int index = 0;
            for(String udata : dataArray) {
                String[] parameters = udata.split("[\\*#]");

                int userId = Integer.parseInt(parameters[0]);
                String username = parameters[1];
                boolean administrator = udata.matches("[0-9]+\\*.*");

                User user = new User(userId, username, administrator);
                users[index++] = user;
            }

            helper.addParameter(Intents.EXTRA_USER_LIST, users);
        }

        helper.send(remoteService);
    }

    public void requestCreateUser(String username, String passwordHash) {
        queue.offer(new Command(Header.MSG_A_USER_CREATE, username + ";" + passwordHash));
    }

    public void requestEditUser(int userId, String username, String passwordHash) {
        queue.offer(new Command(Header.MSG_A_USER_EDIT, userId + ";" + username + ";" + passwordHash));
    }

    public void requestDeleteUser(int userId) {
        queue.offer(new Command(Header.MSG_A_USER_DELETE, Integer.toString(userId)));
    }

    /**
     * Processes an incoming packet that is to be
     * processed asynchronously (not a response of a command).
     */
    protected void processAsynchPacket(Packet packet) {
        switch (packet.getHeader()) {
            case Header.MSG_A_ERROR:
            {
                new BroadcastHelper(Intents.ACTION_CALLBACK).addParameter(Intents.EXTRA_ERROR, packet.getData()).send(remoteService);
                break;
            }
            case Header.MSG_A_USERS_CHANGED:
            {
                new BroadcastHelper(Intents.ACTION_USERS_CHANGED).send(remoteService);
                break;
            }
            case Header.MSG_A_STATE_CHANGED:
            {
                BroadcastHelper helper = new BroadcastHelper(Intents.ACTION_DEVICE_STATE_CHANGED);

                String data = packet.getData();
                try {
                    String[] parameters = data.split(";");
                    if(parameters != null && parameters.length > 0) {
                        String eId = parameters[0];
                        Entity[] eOutput = new Entity[1];
                        Entity.deserialize(data, 0, eOutput);
                        helper.addParameter(Intents.EXTRA_DEVICE_STATE, eOutput[0]);
                    } else {
                        helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_parse_changed_entity) + ": " + data);
                    }
                } catch (Exception ex) {
                    helper.addParameter(Intents.EXTRA_ERROR, remoteService.getResources().getString(R.string.error_parse_changed_entity) + ": " + data + " | " + ex);
                }

                helper.send(remoteService);
                break;
            }
            default: break;
        }
    }

    /** Loads the image of the given entity type asynchronously (from a background thread). */
    private void loadImage(EntityType type) {
        if(type.isImageSet()) return;

        handler.send(Header.MSG_A_LOAD_TYPE_IMAGE, type.getImageFilename());

        Packet response = handler.poll(Header.MSG_A_LOAD_TYPE_IMAGE, 5000);
        if(response != null) {
            byte[] decoded = Base64.decode(response.getData(), Base64.DEFAULT);
            BitmapDrawable drawable = new BitmapDrawable(remoteService.getResources(), BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            type.setImage(drawable);
        }
    }

    /** Helper class to send broadcasts to BroadcastReceivers. */
    private class BroadcastHelper {

        /** The intent object to send. */
        private final Intent intent;
        /** The parameters of the intent. */
        private final Bundle bundle = new Bundle();

        /**
         * Private constructor.
         * @param action The action of the intent in broadcast
         */
        private BroadcastHelper(String action) {
            this.intent = new Intent(action);
        }

        /** Adds a parameter to the intent's Bundle. */
        private BroadcastHelper addParameter(String name, Object value) {
            if(value instanceof Integer) {
                bundle.putInt(name, (Integer) value);
            } else if(value instanceof String) {
                bundle.putString(name, value != null ? value.toString() : null);
            } else if(value instanceof Parcelable) {
                bundle.putParcelable(name, (Parcelable) value);
            } else if(value instanceof Parcelable[]) {
                bundle.putParcelableArray(name, (Parcelable[]) value);
            }

            return this;
        }

        /** Sends the broadcast intent. */
        private void send(Context context) {
            this.intent.putExtras(this.bundle);
            LocalBroadcastManager.getInstance(context).sendBroadcast(this.intent);
        }
    }

}
