package com.file.transfer.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.TextUtils;


public class NetworkStatusReceiver extends BroadcastReceiver {

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int stage = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);

        if (TextUtils.equals(intent.getAction(), "android.net.wifi.WIFI_STATE_CHANGED")) {
            if (this.callback != null) {
                this.callback.onWifiStageChange(stage);
            }
        }

        if (TextUtils.equals(intent.getAction(), "android.net.wifi.WIFI_AP_STATE_CHANGED")) {
            if (this.callback != null) {
                this.callback.onHotspotStageChange(stage);
            }
        }

/*        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
            } else {
                // Wi-Fi P2P is not enabled
            }
        }*/
    }

    public interface Callback {

        void onHotspotStageChange(int stage);

        void onWifiStageChange(int stage);
    }

}
