package hu.rycus.intellihome;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hu.rycus.intellihome.model.EntityHistory;
import hu.rycus.intellihome.network.RemoteManager;

/**
 * Service implementation responsible for feeding UI components
 * with data from the server.
 *
 * Created by Viktor Adam on 12/4/13.
 */
public class RemoteService extends Service {

    /** The identifier used for the persistent notification. */
    private static final int NOTIFICATION_ID = 1;

    /** The instance of the binder implementation. */
    private final IBinder binder = new RemoteBinder();
    /** Reference counter for bound clients. */
    private final AtomicInteger bindCount = new AtomicInteger(0);

    /** Lock object around start/stop operations. */
    private Lock startStopLock = new ReentrantLock();
    /** The current remote manager object. */
    private RemoteManager remoteManager = null;

    /** Creates a persistent notification which brings the service to foreground. */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(getResources().getString(R.string.app_name));
        builder.setContentText(getString(R.string.notification_content));
        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        return builder.build();
    }

    /** Starts the remote manager. */
    private void start() {
        startStopLock.lock();
        try {
            if(remoteManager == null) {
                remoteManager = RemoteManager.start(this);

                startForeground(NOTIFICATION_ID, createNotification());
            }
        } finally {
            startStopLock.unlock();
        }
    }

    /** Stops the remote manager. */
    private void stop() {
        startStopLock.lock();
        try {
            if(remoteManager != null) {
                remoteManager.shutdown();
                remoteManager = null;

                stopForeground(true);
            }
        } finally {
            startStopLock.unlock();
        }
    }

    public void restartCommunication() {
        startStopLock.lock();
        try {
            stop();
            start();
        } finally {
            startStopLock.unlock();
        }
    }

    /** Returns true if the remote manager has a valid connection to the server. */
    public boolean isConnected() {
        return remoteManager != null && remoteManager.isConnected();
    }

    /** Returns true, if the authenticated user is the administrator. */
    public boolean isAdministratorUser() {
        return isConnected() && remoteManager.isAdministratorUser();
    }

    /* Requests for protocol messages. */

    public void requestDeviceTypeList() {
        remoteManager.requestDeviceTypeList();
    }

    public void requestDeviceList(Integer type) {
        remoteManager.requestDeviceList(type);
    }

    public void sendCommand(String entityId, int commandId, String parameter) {
        remoteManager.sendCommand(entityId, commandId, parameter);
    }

    public void renameDevice(String entityId, String name) {
        remoteManager.renameDevice(entityId, name);
    }

    public int countHistory(Long tsFrom, Long tsTo, String entityId) {
        return remoteManager.countHistory(tsFrom, tsTo, entityId);
    }

    public EntityHistory[] listHistory(Long tsFrom, Long tsTo, String entityId, int limit, int offset) {
        return remoteManager.listHistory(tsFrom, tsTo, entityId, limit, offset);
    }

    public void requestUserList() {
        remoteManager.requestUserList();
    }

    public void requestCreateUser(String username, String passwordHash) {
        remoteManager.requestCreateUser(username, passwordHash);
    }

    public void requestEditUser(int userId, String username, String passwordHash) {
        remoteManager.requestEditUser(userId, username, passwordHash);
    }

    public void requestDeleteUser(int userId) {
        remoteManager.requestDeleteUser(userId);
    }

    /** This runs when the first client binds to the service. */
    private void onFirstBind() {
        startService(new Intent(this, RemoteService.class));
        start();
    }

    /** This runs when the last client unbinds. */
    private void onLastUnbind() {
        stop();
        stopSelf();
    }

    /** @see android.app.Service#onBind(android.content.Intent) */
    @Override
    public IBinder onBind(Intent intent) {
        int clients = bindCount.incrementAndGet();
        if(clients == 1) {
            onFirstBind();
        }

        return binder;
    }

    /** @see android.app.Service#onUnbind(android.content.Intent) */
    @Override
    public boolean onUnbind(Intent intent) {
        int clients = bindCount.decrementAndGet();
        if(clients == 0) {
            onLastUnbind();
        }

        return super.onUnbind(intent);
    }

    /** Service binder implementation returning the local service instance. */
    public class RemoteBinder extends Binder {
        /** Returns a reference to the bound service. */
        public RemoteService getService() {
            return RemoteService.this;
        }
    }

}
