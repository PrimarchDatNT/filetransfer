package com.file.transfer.core;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.file.transfer.R;
import com.file.transfer.core.fragment.ConectionFragment;
import com.file.transfer.core.fragment.ReceiveFragment;
import com.file.transfer.core.fragment.TransferManagerFragment;
import com.file.transfer.core.fragment.TransferOptionFragment;
import com.file.transfer.core.receive.ScanQrActivity;
import com.file.transfer.core.server.TransferType;
import com.file.transfer.core.util.AppKeyConstant;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.ActivityTransferMangerBinding;
import com.file.transfer.databinding.DialogRenameLibararyBinding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class TransferMangerActivity extends AppCompatActivity implements TransferOptionFragment.Callback,
        CommunicationService.Callback, TransferManagerFragment.Callback, ReceiveFragment.Callback, NetworkStatusReceiver.Callback {

    private static final int REQ_LOCAL_PMS_CODE = 4095;
    private static final int REQ_WRITE_SETTING_PMS_CODE = 4096;
    private static final long CAN_BACK_TIME_DELAY = 10000;
    private final Handler handler = new Handler();
    private String serverAddress;
    private boolean isBound;
    private boolean onAppEvent;
    @TransferType
    private int transferType;
    private Fragment[] arrFragment;
    private MAdapter tabAdapter;
    private ArrayList<String> sharePaths;
    private ArrayList<String> uploadedPaths;
    private CommunicationService mService;
    private ServiceConnection connection;
    private ActivityTransferMangerBinding mBinding;
    private NetworkStatusReceiver networkReceiver;
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private boolean canBackOnConnecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initData();
        this.initLayout();
        this.initContent();
        this.initReceiver();
        this.initWifiDirect();
    }

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    WifiP2pManager.PeerListListener myPeerListListener;

    private void initWifiDirect() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
//        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
    }

    private void initData() {
        Intent intent = this.getIntent();
        if (intent == null) {
            this.finish();
            return;
        }

        this.connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                isBound = true;
                CommunicationService.CommunicateBinder binder = (CommunicationService.CommunicateBinder) service;
                mService = binder.getService();
                onConnectedService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        this.transferType = intent.getIntExtra(ConnectUtils.EXTRA_TRANSFER_TYPE, TransferType.DEFAULT);
        this.sharePaths = intent.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH);
    }

    private void initLayout() {
        this.mBinding = ActivityTransferMangerBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.mBinding.getRoot());
    }

    private void initContent() {
        this.mBinding.tvTitle.setText(R.string.file_trasnfer);
        this.arrFragment = new Fragment[]{TransferOptionFragment.newInstance(this)};
        this.showSmartTab();
        this.tabAdapter = new MAdapter(this.getSupportFragmentManager(), FragmentStatePagerAdapter.POSITION_NONE);
        this.mBinding.vpContent.setAdapter(this.tabAdapter);
        this.mBinding.vpContent.setOffscreenPageLimit(2);
        this.mBinding.smToolbar.setViewPager(this.mBinding.vpContent);
    }

    private void initReceiver() {
        this.networkReceiver = new NetworkStatusReceiver();
        this.networkReceiver.setCallback(this);
        try {
            this.registerReceiver(this.networkReceiver, ConnectUtils.configWifiStageFilter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickRestartSever(View view) {
        if (this.transferType == TransferType.WIFI_SHARE) {
            if (AppUtil.isNetworkConnected(this)) {
                this.updateWifiShare();
            } else {
                this.onEnableWifi();
            }
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            this.onShareHotspot();
        }
    }

    public void updateWifiShare() {
        if (this.mService == null) {
            this.startWebSerivce();
        } else {
            this.mService.startServer();
            this.mService.updateNotification(true);
        }

        Fragment fragment = this.arrFragment[0];

        if (fragment instanceof ConectionFragment) {
            ((ConectionFragment) fragment).showErrorLayout(false);
            ((ConectionFragment) fragment).updateAddressStage(this.serverAddress);
        }

        this.mBinding.clRestartServer.setVisibility(View.GONE);
    }

    public void onClickRefresh(View view) {
        Fragment fragment = this.arrFragment[0];
        if (this.transferType != TransferType.RECEIVE || fragment == null) {
            return;
        }

        if (fragment instanceof ReceiveFragment) {
            ((ReceiveFragment) fragment).loadData();
        }
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }

    private boolean isShowTab() {
        return this.arrFragment.length > 1;
    }

    @Override
    public void onShareWifi() {
        if (!AppUtil.isNetworkConnected(this)) {
            this.showEnableWifiDialog();
            return;
        }

        this.transferType = TransferType.WIFI_SHARE;
        TransferManagerFragment transferManagerFragment = TransferManagerFragment.newInstance(this.sharePaths, this.uploadedPaths);
        transferManagerFragment.setCallback(this);
        this.arrFragment = new Fragment[]{
                ConectionFragment.newInstance("", ""),
                transferManagerFragment
        };

        this.showSmartTab();
        this.tabAdapter.notifyDataSetChanged();
        this.mBinding.smToolbar.setViewPager(this.mBinding.vpContent);
        this.mBinding.tvTitle.setText(R.string.send_via_wifi);
        this.bindService(new Intent(this, CommunicationService.class), this.connection, Context.BIND_AUTO_CREATE);
        this.startWebSerivce();
    }

    @Override
    public void onShareHotspot() {
        if (!ConnectUtils.isLocationEnabled(this)) {
            Toast.makeText(this, R.string.enable_location_service_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!this.availableWriteSettingPms()) {
            this.showPermissionDialog(REQ_WRITE_SETTING_PMS_CODE);
            return;
        }

        if (this.unableHotspotPms()) {
            this.reqHotspotPms();
            return;
        }

        this.turnOnHotspot();
    }

    private void onActivedHotot(String ssid, String password) {
        if (TextUtils.isEmpty(ssid)) {
            Toast.makeText(this, R.string.err_start_hotspot, Toast.LENGTH_SHORT).show();
            return;
        }

        this.transferType = TransferType.HOTSPOT_SHARE;

        TransferManagerFragment transferManagerFragment = TransferManagerFragment.newInstance(this.sharePaths, this.uploadedPaths);
        transferManagerFragment.setCallback(this);
        this.arrFragment = new Fragment[]{
                ConectionFragment.newInstance(ssid, password),
                transferManagerFragment
        };

        this.showSmartTab();
        this.tabAdapter.notifyDataSetChanged();
        this.mBinding.smToolbar.setViewPager(this.mBinding.vpContent);
        this.mBinding.tvTitle.setText(R.string.send_with_hotspot);
        this.bindService(new Intent(this, CommunicationService.class), this.connection, Context.BIND_AUTO_CREATE);
        this.startWebSerivce();
    }

    @SuppressLint("MissingPermission")
    private void turnOnHotspot() {
        WifiManager manager = ConnectUtils.getWifiManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mBinding.flLoading.setVisibility(View.VISIBLE);


            if (this.isEnableHotspot()) {
                this.turnOffHotspot();
            }

            this.canBackOnConnecting = false;
            this.handler.postDelayed(() -> canBackOnConnecting = true, CAN_BACK_TIME_DELAY);

            try {
                manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        mBinding.flLoading.setVisibility(View.GONE);
                        mBinding.clRestartServer.setVisibility(View.GONE);
                        mReservation = reservation;
                        WifiConfiguration wifiConfiguration = mReservation.getWifiConfiguration();
                        onActivedHotot(wifiConfiguration.SSID, wifiConfiguration.preSharedKey);
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        mBinding.flLoading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailed(int reason) {
                        super.onFailed(reason);
                        mBinding.flLoading.setVisibility(View.GONE);
                    }

                }, new Handler(Looper.getMainLooper()));

            } catch (Exception e) {
                Toast.makeText(this, R.string.enable_location_service_msg, Toast.LENGTH_SHORT).show();
                mBinding.flLoading.setVisibility(View.GONE);
                e.printStackTrace();
            }
            return;
        }

        if (this.isEnableHotspot()) {
            this.disableConfigured();
        }

        this.enableConfigured(manager);
    }

    private void enableConfigured(@NotNull WifiManager wifiManager) {
        this.mBinding.flLoading.setVisibility(View.VISIBLE);

        wifiManager.setWifiEnabled(false);
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = Build.MODEL;
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        for (Method method : WifiManager.class.getDeclaredMethods()) {
            if (TextUtils.equals(method.getName(), "setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, wifiConfiguration, true);
                    this.onAppEvent = true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isEnableHotspot() {
        WifiManager manager = ConnectUtils.getWifiManager(this);
        if (manager == null) {
            return false;
        }

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return false;
        }

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "isWifiApEnabled")) {
                try {
                    Boolean isEnable = (Boolean) method.invoke(manager);
                    return isEnable != null && isEnable;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOffHotspot() {
        if (this.mReservation != null) {
            this.mReservation.close();
            this.mReservation = null;
        }

        WifiManager manager = ConnectUtils.getWifiManager(this);

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return;
        }

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "cancelLocalOnlyHotspotRequest")) {
                try {
                    method.invoke(manager);
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            if (TextUtils.equals(method.getName(), "stopLocalOnlyHotspot")) {
                try {
                    method.invoke(manager);
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void disableConfigured() {
        WifiManager wifiManager = ConnectUtils.getWifiManager(this);
        if (wifiManager == null) {
            return;
        }

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return;
        }

        Method setHotspotEnable = null;

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "setWifiApEnabled")) {
                setHotspotEnable = method;
                break;
            }
        }

        WifiConfiguration configuration = ConnectUtils.getNetworkConfig(wifiManager);

        if (setHotspotEnable == null || configuration == null) {
            return;
        }

        try {
            setHotspotEnable.invoke(wifiManager, configuration, false);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInputAddress() {
        DialogRenameLibararyBinding binding = DialogRenameLibararyBinding.inflate(this.getLayoutInflater());
        Dialog dialog = AppUtil.createDialog(this);
        dialog.setContentView(binding.getRoot());

        binding.tvDialogCancelCompressTitle.setText(R.string.input_address_title);
        binding.tvConfirmRename.setText(R.string.cta_connect);
        binding.edFileRename.setText(ConnectUtils.HTTP_HEADER);
        binding.edFileRename.setSelection(ConnectUtils.HTTP_HEADER.length());

        binding.tvCancelRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                AppUtil.hideKeyboard(TransferMangerActivity.this, binding.edFileRename);
                dialog.dismiss();
            }
        });

        binding.tvConfirmRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                Editable text = binding.edFileRename.getText();
                if (text == null || TextUtils.isEmpty(text.toString().trim())) {
                    Toast.makeText(TransferMangerActivity.this, R.string.empty_input_address_msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                AppUtil.hideKeyboard(TransferMangerActivity.this, binding.edFileRename);
                dialog.dismiss();
                onInputResult(text.toString());
            }
        });

        AppUtil.showKeyboard(this);
        AppUtil.showFullScreenDialog(dialog);
    }

    @Override
    public void onScan() {
        this.startActivityForResult(new Intent(this, ScanQrActivity.class), ConnectUtils.REQ_SCAN_QR);
    }

    @Override
    public void onSelectUploadFile() {
        Intent intent = new Intent(this, SelectFileTransferActivity.class);
        intent.putExtra(ConnectUtils.EXTRA_SELECT_ONLY_FILE, true);
        this.startActivityForResult(intent, ConnectUtils.REQ_SELECT_UPLOAD_FILE);
    }

    @Override
    public void onEnableWifi() {
        this.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), ConnectUtils.REQ_ENABLE_WIFI);
    }

    @Override
    public void onScanAddress() {
        this.onScan();
    }

    private void showEnableWifiDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Check wifi")
                .setMessage("Please enable wifi to use this feature!")
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.go_setting, (dialog, which) -> {
                    dialog.dismiss();
                    onEnableWifi();
                })
                .create()
                .show();
    }

    private void startWebSerivce() {
        Intent iService = new Intent(this, CommunicationService.class);
        iService.putStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH, this.sharePaths);
        this.startService(iService);
    }

    private void showSmartTab() {
        this.mBinding.smToolbar.setVisibility(isShowTab() ? View.VISIBLE : View.GONE);
    }

    private void onConnectedService() {
        if (this.mService == null) {
            return;
        }

        this.mService.setCallback(this);
        this.serverAddress = this.mService.getServerAddress();

        if (this.transferType == TransferType.WIFI_SHARE || this.transferType == TransferType.HOTSPOT_SHARE) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Fragment fragment = this.arrFragment[0];
                if (fragment instanceof ConectionFragment) {
                    ((ConectionFragment) fragment).updateAddressStage(this.serverAddress);
                }
            }, 500);
        }
    }

    private void showExitDialog() {
        String message = this.getString(R.string.exit_feature_msg);

        if (this.transferType == TransferType.WIFI_SHARE) {
            message = this.getString(R.string.exit_wifi_feature_msg);
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            message = this.getString(R.string.exit_hotspot_feature_msg);
        }

        if (this.transferType == TransferType.RECEIVE) {
            message = this.getString(R.string.exit_recieve_feaute);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_feature)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    disconnectService();
                    showOptionTransfer();
                })
                .create()
                .show();
    }

    private void showOptionTransfer() {
        this.arrFragment = new Fragment[]{TransferOptionFragment.newInstance(this)};
        this.showSmartTab();
        this.tabAdapter.notifyDataSetChanged();
        this.mBinding.clRestartServer.setVisibility(View.GONE);
        this.mBinding.ivRefresh.setVisibility(View.GONE);
        this.mBinding.smToolbar.setVisibility(View.GONE);

        if (this.sharePaths != null) {
            this.sharePaths.clear();
        }

        if (this.uploadedPaths != null) {
            this.uploadedPaths.clear();
        }

        this.transferType = TransferType.DEFAULT;
        this.mBinding.tvTitle.setText(R.string.file_trasnfer);
    }

    @Override
    public void onBackPressed() {
        if (this.transferType != TransferType.DEFAULT) {
            this.showExitDialog();
            return;
        }
        if (mBinding.flLoading.getVisibility() == View.GONE || canBackOnConnecting) {
            this.removeAllHandler();
            super.onBackPressed();
        }
    }

    private void removeAllHandler() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onReceiveItem(ArrayList<String> uploadPaths) {
        this.uploadedPaths = new ArrayList<>(uploadPaths);
        TransferManagerFragment managerFragment = (TransferManagerFragment) this.arrFragment[1];
        if (managerFragment != null) {
            managerFragment.setListReceive(this.uploadedPaths);
        }
    }

    @Override
    public void onStopService() {
        this.disconnectService();

        if (this.transferType == TransferType.WIFI_SHARE || this.transferType == TransferType.HOTSPOT_SHARE) {
            this.showOptionTransfer();
        }
    }

    @Override
    public void onRemoveItem(boolean isSendItem, int position) {
        if (isSendItem) {
            if (this.sharePaths == null || this.sharePaths.isEmpty()) {
                return;
            }

            this.sharePaths.remove(position);

            if (this.mService != null) {
                this.mService.updateShareList(this.sharePaths);
            }

            return;
        }

        if (this.uploadedPaths == null || this.uploadedPaths.isEmpty()) {
            return;
        }

        String path = this.uploadedPaths.get(position);
        if (TextUtils.isEmpty(path)) {
            return;
        }

        File file = new File(path);
        if (file.exists()) {
            System.out.println("Deleted: " + file.delete());
        }

        this.uploadedPaths.remove(position);

        boolean updateShare = false;
        if (this.sharePaths != null) {
            for (int i = 0; i < this.sharePaths.size(); i++) {
                if (TextUtils.equals(path, this.sharePaths.get(i))) {
                    updateShare = true;
                    this.sharePaths.remove(i);
                    break;
                }
            }
        }

        if (this.mService != null) {
            this.mService.setUploadedPaths(this.uploadedPaths);
            if (updateShare) {
                this.updateSendFiles();
            }
        }
    }

    public void updateSendFiles() {
        this.mService.updateShareList(this.sharePaths);
        TransferManagerFragment managerFragment = (TransferManagerFragment) this.arrFragment[1];
        if (managerFragment == null) {
            return;
        }

        managerFragment.setListShare(this.sharePaths);
    }

    @Override
    public void onSelectFileShare() {
        Intent intent = new Intent(this, SelectFileTransferActivity.class);
        intent.putStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH, this.sharePaths);
        this.startActivityForResult(intent, ConnectUtils.REQ_SELECT_FILE_SHARE_CODE);

//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//        this.startActivityForResult(intent, ConnectUtils.REQ_SELECT_FILE_SHARE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCAL_PMS_CODE) {
            SharedPreferences preferences = this.getSharedPreferences("rar-extractor-prefs", Context.MODE_PRIVATE);

            for (String permission : permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                        if (permission.equals(ACCESS_FINE_LOCATION)) {
                            this.onShareHotspot();
                            return;
                        }
                    } else if (permission.equals(ACCESS_FINE_LOCATION)) {
                        preferences.edit().putBoolean("k_dont_ask_location", true).apply();
                    }
                }
            }

            if (preferences.getBoolean("k_dont_ask_location", false)) {
                this.showPermissionDialog(REQ_LOCAL_PMS_CODE);
            }
        }
    }

    private boolean availableWriteSettingPms() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
        }
        return retVal;
    }

    private void showPermissionDialog(int reqCode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_permission)
                .setMessage(reqCode == REQ_WRITE_SETTING_PMS_CODE ? R.string.write_setting_pms_msg : R.string.hotspot_pms_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.go_setting, (dialog, which) -> {
                    dialog.dismiss();
                    goSetting(reqCode);
                })
                .create()
                .show();
    }

    private void goSetting(int reqCode) {
        final Intent intent = new Intent();
        if (reqCode == REQ_LOCAL_PMS_CODE) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }

        if (reqCode == REQ_WRITE_SETTING_PMS_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            }
            intent.setData(Uri.parse("package:" + this.getPackageName()));
        }
        this.startActivityForResult(intent, reqCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == ConnectUtils.REQ_SELECT_FILE_SHARE_CODE) {
            if (data == null || this.transferType == TransferType.DEFAULT) {
                return;
            }

            ArrayList<String> paths = data.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH);
            if (paths == null || paths.isEmpty()) {
                this.sharePaths = new ArrayList<>();
            } else {
                this.sharePaths = new ArrayList<>(paths);
            }

            this.updateSendFiles();
            return;
        }

        if (resultCode == Activity.RESULT_OK && requestCode == ConnectUtils.REQ_SCAN_QR) {
            this.onInputResult(data == null ? "" : data.getStringExtra(ConnectUtils.EXTRA_SCAN_RESULT));
            return;
        }

        if (resultCode == Activity.RESULT_OK && requestCode == ConnectUtils.REQ_SELECT_UPLOAD_FILE) {
            if (data == null) {
                return;
            }

            this.startUploadFile(data.getStringExtra(AppKeyConstant.EXTRA_BROWSE_RESULT));
            return;
        }

        if (requestCode == ConnectUtils.REQ_ENABLE_WIFI) {
            if (AppUtil.isNetworkConnected(this)) {

                if (this.transferType == TransferType.DEFAULT) {
                    this.onShareWifi();
                    return;
                }

                if (this.transferType == TransferType.WIFI_SHARE) {
                    this.updateWifiShare();
                }

                if (this.transferType == TransferType.RECEIVE) {
                    Fragment fragment = this.arrFragment[0];

                    if (fragment instanceof ReceiveFragment) {
                        ((ReceiveFragment) fragment).loadData();
                    }
                }
            }

            return;
        }

        if (requestCode == REQ_LOCAL_PMS_CODE || requestCode == REQ_WRITE_SETTING_PMS_CODE) {
            this.onShareHotspot();
        }
    }

    @SuppressLint("SetTextI18n")
    private void onInputResult(String scanResult) {
        if (scanResult.startsWith("WIFI:")) {
            connectToWifi(scanResult);
            return;
        }

        if (this.transferType == TransferType.RECEIVE) {
            Fragment fragment = this.arrFragment[0];
            if (fragment instanceof ReceiveFragment) {
                ((ReceiveFragment) fragment).setServerAddress(scanResult);
                ((ReceiveFragment) fragment).loadData();
            }
            return;
        }

        this.transferType = TransferType.RECEIVE;
        ReceiveFragment fragment = ReceiveFragment.newInstance(scanResult);
        fragment.setCallback(this);
        this.arrFragment = new Fragment[]{fragment};
        this.tabAdapter.notifyDataSetChanged();
        this.mBinding.tvTitle.setText(R.string.feature_receive);
        this.mBinding.ivRefresh.setVisibility(View.VISIBLE);
    }

    private void connectToWifi(String wifiProperty) {
        String ssid = wifiProperty.split("S:")[1].split(";")[0];
        String password = wifiProperty.split("P:")[1].split(";")[0];
        this.serverAddress = wifiProperty.split("IP:")[1].split(";")[0];
        this.transferType = TransferType.RECEIVE;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          /*  final WifiNetworkSuggestion wifiNetworkSuggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(password)
                            .setIsAppInteractionRequired(true)
                            .build();


            final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
            suggestionsList.add(wifiNetworkSuggestion);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                final Intent intent;
                intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
                intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, (ArrayList<? extends Parcelable>) suggestionsList);
                startActivityForResult(intent, 123);
            } else {
                final WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.addNetworkSuggestions(suggestionsList);
                final Intent intent;
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivityForResult(intent, 123);
            }*/

            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid(ssid);
            builder.setWpa2Passphrase(password);

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();

            NetworkRequest.Builder netwReqBuilder = new NetworkRequest.Builder();
            netwReqBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
            netwReqBuilder.setNetworkSpecifier(wifiNetworkSpecifier);

            NetworkRequest nr = netwReqBuilder.build();
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    cm.bindProcessToNetwork(network);
                    onConnectedHotpot();
                }
            };
            cm.requestNetwork(nr, networkCallback);
        } else {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", password);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();

            this.mBinding.getRoot().postDelayed(() -> {
                ReceiveFragment fragment = ReceiveFragment.newInstance(serverAddress);
                fragment.setCallback(this);
                this.arrFragment = new Fragment[]{fragment};
                this.tabAdapter.notifyDataSetChanged();
                this.mBinding.tvTitle.setText(R.string.feature_receive);
                this.mBinding.ivRefresh.setVisibility(View.VISIBLE);
            }, 2000);
        }
    }

    private void onConnectedHotpot() {
        this.mBinding.getRoot().postDelayed(() -> {
            ReceiveFragment fragment = ReceiveFragment.newInstance(serverAddress);
            fragment.setCallback(this);
            this.arrFragment = new Fragment[]{fragment};
            this.tabAdapter.notifyDataSetChanged();
            this.mBinding.tvTitle.setText(R.string.feature_receive);
            this.mBinding.ivRefresh.setVisibility(View.VISIBLE);
        }, 2000);

    }

    private void startUploadFile(String path) {
        ReceiveFragment fragment = (ReceiveFragment) this.arrFragment[0];
        if (fragment == null) {
            return;
        }

        fragment.uploadFileToServer(path);
    }

    private boolean unableHotspotPms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private void reqHotspotPms() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQ_LOCAL_PMS_CODE);
    }

    private void disconnectService() {
        if (this.isBound && this.connection != null) {
            try {
                this.unbindService(this.connection);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (this.mService != null) {
                this.mService.stopSelf();
            }
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.turnOffHotspot();
            } else {
                this.disableConfigured();
            }
        }
    }

    @Override
    protected void onDestroy() {
        this.removeAllHandler();
        this.unregisterReceiver();
        this.disconnectService();
        System.gc();
        super.onDestroy();
    }

    private void unregisterReceiver() {
        if (this.networkReceiver != null) {
            try {
                this.unregisterReceiver(this.networkReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onHotspotStageChange(int stage) {
        if (stage == ConnectUtils.WIFI_AP_STATE_DISABLED) {
            if (this.transferType != TransferType.HOTSPOT_SHARE) {
                return;
            }

            if (this.mService != null) {
                this.mService.stopServer();
                this.mService.updateNotification(false);
            }

            this.mBinding.clRestartServer.setVisibility(View.VISIBLE);
        }

        if (stage == ConnectUtils.WIFI_AP_STATE_FAILED) {
            this.mBinding.flLoading.setVisibility(View.GONE);
        }

        if (stage == ConnectUtils.WIFI_AP_STATE_ENABLED) {
            if (this.onAppEvent) {
                WifiManager wifiManager = ConnectUtils.getWifiManager(this);
                WifiConfiguration configuration = ConnectUtils.getNetworkConfig(wifiManager);
                if (configuration == null) {
                    return;
                }

                if (this.transferType == TransferType.DEFAULT) {
                    this.onActivedHotot(configuration.SSID, configuration.preSharedKey);
                    this.onAppEvent = false;
                }

                if (this.transferType == TransferType.HOTSPOT_SHARE) {
                    if (this.mService == null) {
                        this.startWebSerivce();
                    } else {
                        this.mService.startServer();
                        this.mService.updateNotification(true);
                    }

                    Fragment fragment = this.arrFragment[0];
                    if (fragment instanceof ConectionFragment) {
                        ((ConectionFragment) fragment).showErrorLayout(false);
                        ((ConectionFragment) fragment).updateHotspotStage(configuration.SSID, configuration.preSharedKey);
                    }

                    this.mBinding.clRestartServer.setVisibility(View.GONE);
                }

                this.mBinding.flLoading.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onWifiStageChange(int stage) {
        if (stage == ConnectUtils.WIFI_STATE_DISABLED || stage == ConnectUtils.WIFI_STATE_UNKNOWN) {
            if (this.transferType != TransferType.WIFI_SHARE) {
                return;
            }

            if (this.mService != null) {
                this.mService.stopServer();
                this.mService.updateNotification(false);
            }

            this.mBinding.clRestartServer.setVisibility(View.VISIBLE);
            Fragment fragment = this.arrFragment[0];
            if (fragment instanceof ConectionFragment) {
                ((ConectionFragment) fragment).showErrorLayout(true);
            }
        }
    }

    private final class MAdapter extends FragmentStatePagerAdapter {

        public MAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            if (object instanceof ConectionFragment
                    || object instanceof TransferManagerFragment
                    || object instanceof TransferOptionFragment
                    || object instanceof ReceiveFragment) {
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
            }
        }

        @NotNull
        @Override
        public CharSequence getPageTitle(int position) {
            if (isShowTab()) {
                if (position == 0) {
                    return getString(R.string.connection);
                }

                return getString(R.string.transfer_manager);
            }

            return "Nothing";
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return arrFragment[position];
        }

        @Override
        public int getCount() {
            return isShowTab() ? 2 : 1;
        }
    }

}