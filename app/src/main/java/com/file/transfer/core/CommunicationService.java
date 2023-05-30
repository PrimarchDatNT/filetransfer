package com.file.transfer.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.file.transfer.R;
import com.file.transfer.core.event.UpdateEventTag;
import com.file.transfer.core.event.UpdateFileEvent;
import com.file.transfer.core.server.WebShareServer;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CommunicationService extends Service {

    public static final String ACTION_NEW_SHARE_LIST = "action_new_share_list";
    public static final String ACTION_START_SERVER = "action_start_server";
    public static final String ACTION_STOP_SERVER = "action_stop_server";
    public static final String ACTION_RECEIVE_NEW_FILE = "action_receive_new_file";

    private static final int NOTFICATION_ID = 8132;

    private final IBinder binder = new CommunicateBinder();

    private CommunicateReceiver mReceiver;
    private WebShareServer webShareServer;

    private NotificationManager mNotifiManager;
    private NotificationCompat.Builder mNotifiBuilder;

    private Callback callback;

    private int serverPort;

    private ArrayList<String> uploadedPaths;

    public CommunicationService() {
    }

    public void setUploadedPaths(ArrayList<String> uploadedPaths) {
        this.uploadedPaths = uploadedPaths;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String getServerAddress() {
        return ConnectUtils.HTTP_HEADER + ConnectUtils.getLocalIpAddress() + ":" + this.serverPort;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.init();
    }

    private void init() {
        this.initWebServer();
        this.initReciver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            this.startServer();

            ArrayList<String> sharePaths = intent.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH);
            if (sharePaths != null && !sharePaths.isEmpty()) {
                this.updateShareList(sharePaths);
            }

            this.onStartServer();

            if (TextUtils.equals(intent.getAction(), ACTION_STOP_SERVER)) {
                if (this.mNotifiManager != null) {
                    this.mNotifiManager.cancel(NOTFICATION_ID);
                }

                if (this.callback != null) {
                    this.callback.onStopService();
                }

                this.stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    public void updateNotification(boolean onStart) {
        if (this.mNotifiBuilder == null) {
            return;
        }

        this.mNotifiBuilder.setContentText(onStart ? this.getString(R.string.web_notifi_msg, ConnectUtils.getLocalIpAddress(), this.serverPort) : this.getString(R.string.interrupt_server));

        if (this.mNotifiManager != null) {
            this.mNotifiManager.notify(NOTFICATION_ID, this.mNotifiBuilder.build());
        } else {
            NotificationManagerCompat.from(this).notify(NOTFICATION_ID, this.mNotifiBuilder.build());
        }
    }

    private void onStartServer() {
        NotificationChannel serviceChannel = null;
        String channedId = "WebServiceChannel";
        String channelName = "WebForegroundServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannel = new NotificationChannel(
                    channedId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mNotifiManager = this.getSystemService(NotificationManager.class);
        }

        if (this.mNotifiManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mNotifiManager.createNotificationChannel(serviceChannel);
            }
        }

        try {
            Intent iContent = new Intent(this, TransferMangerActivity.class);

            this.mNotifiBuilder = new NotificationCompat.Builder(this, channedId);
            this.mNotifiBuilder.setOngoing(true)
                    .setContentTitle(this.getString(R.string.web_notifi_title))
                    .setContentText(this.getString(R.string.web_notifi_msg, ConnectUtils.getLocalIpAddress(), this.serverPort))
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, this.getString(R.string.web_notifi_action), this.getServiceIntent(this))
                    .setContentIntent(PendingIntent.getActivity(this, 2059, iContent, 0))
                    .setSmallIcon(android.R.drawable.ic_menu_share);
            this.startForeground(NOTFICATION_ID, this.mNotifiBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PendingIntent getServiceIntent(Context context) {
        Intent intent = new Intent(context, CommunicationService.class);
        intent.setAction(CommunicationService.ACTION_STOP_SERVER);
        return PendingIntent.getService(context, 2539, intent, 0);
    }

    private void initWebServer() {
        this.serverPort = ConnectUtils.findFreePort();
        this.webShareServer = new WebShareServer(this.serverPort, this);
    }

    private void initReciver() {
        this.uploadedPaths = new ArrayList<>();
        this.mReceiver = new CommunicateReceiver();
        IntentFilter intentFilter = ConnectUtils.configWifiStageFilter();
        intentFilter.addAction(ACTION_NEW_SHARE_LIST);
        intentFilter.addAction(ACTION_START_SERVER);
        intentFilter.addAction(ACTION_STOP_SERVER);
        intentFilter.addAction(ACTION_RECEIVE_NEW_FILE);
        this.registerReceiver(this.mReceiver, intentFilter);
    }

    public void startServer() {
        if (this.webShareServer == null || this.webShareServer.isAlive()) {
            return;
        }

        try {
            this.webShareServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (this.webShareServer != null) {
            this.webShareServer.stop();
        }
    }

    public void updateShareList(ArrayList<String> sharePaths) {
        if (this.webShareServer == null) {
            return;
        }
        this.webShareServer.setSharePaths(sharePaths);
    }

    @Override
    public void onDestroy() {
        try {
            this.unregisterReceiver(this.mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.stopServer();
        this.stopForeground(true);
        super.onDestroy();
    }

    public interface Callback {

        void onReceiveItem(ArrayList<String> uploadPaths);

        void onStopService();
    }

    public class CommunicateBinder extends Binder {
        CommunicationService getService() {
            return CommunicationService.this;
        }
    }

    public class CommunicateReceiver extends BroadcastReceiver {

        final CommunicationService mService = CommunicationService.this;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            if (TextUtils.equals(intent.getAction(), ACTION_START_SERVER)) {
                this.mService.startServer();
            }

            if (TextUtils.equals(intent.getAction(), ACTION_STOP_SERVER)) {
                if (this.mService.callback != null) {
                    this.mService.callback.onStopService();
                }
                this.mService.stopSelf();
            }

            if (TextUtils.equals(intent.getAction(), ACTION_NEW_SHARE_LIST)) {
                ArrayList<String> sharePaths = intent.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH);
                if (sharePaths == null || sharePaths.isEmpty()) {
                    return;
                }
                this.mService.updateShareList(sharePaths);
            }

            if (TextUtils.equals(intent.getAction(), ACTION_RECEIVE_NEW_FILE)) {

                String filePath = intent.getStringExtra(ConnectUtils.EXTRA_RECEIVE_FILE_PATH);
                if (filePath == null || TextUtils.isEmpty(filePath)) {
                    return;
                }

                this.mService.uploadedPaths.add(filePath);
                this.mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));
                EventBus.getDefault().post(new UpdateFileEvent(UpdateEventTag.TAG_UPDATE_LOCAL_DATA));
                if (this.mService.callback != null) {
                    this.mService.callback.onReceiveItem(this.mService.uploadedPaths);
                }
            }

        }
    }

}