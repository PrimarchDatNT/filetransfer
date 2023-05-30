package com.file.transfer.core;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;

import com.genonbeta.android.framework.io.DocumentFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

public class ConnectUtils {
    public static final int DEFAULT_WEB_SERVER_PORT = 8889;

    public static final int TRASFER_OBJECT_SEND = 1;
    public static final int TRASFER_OBJECT_RECEIVE = 2;
    public static final int TRASFER_OBJECT_DOWNLOAD = 3;
    public static final int TRASFER_OBJECT_UPLOAD = 4;

    public static final int WIFI_STATE_DISABLED = 1;
    /*public static final int WIFI_STATE_ENABLED = 3;*/
    public static final int WIFI_STATE_UNKNOWN = 4;

    /*public static final int WIFI_AP_STATE_DISABLING = 10;*/
    public static final int WIFI_AP_STATE_DISABLED = 11;
    /* public static final int WIFI_AP_STATE_ENABLING = 12;*/
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    public static final int REQ_SELECT_FILE_SHARE_CODE = 135;
    public static final int REQ_SELECT_UPLOAD_FILE = 136;
    public static final int REQ_SCAN_QR = 137;
    /*public static final int REQ_ACCESS_STORAGE_FRAMEWORK = 138;*/
    public static final int REQ_ENABLE_WIFI = 139;

    public static final String EXTRA_SELECT_ONLY_FILE = "extra_select_only_file";
    public static final String EXTRA_SHARE_PATH = "extra_share_path";
    public static final String EXTRA_TRANSFER_TYPE = "extra_transfer_type";
    public static final String EXTRA_SCAN_RESULT = "extra_scan_result";
    public static final String EXTRA_RECEIVE_FILE_PATH = "extra_receive_file_path";
    public static final String AVAILABLE_DOWNLOAD = "Available";
    public static final String UNABLE_DOWNLOAD = "No content";
    public static final String UPLOAD_FAIL_MSG = "Up load fail!";

    public static final String HTTP_HEADER = "http://";

    private static final int MAX_PORT_NUMBER = 49151;

    public static boolean isValidUrl(@NotNull String input) {
        if (input.isEmpty()) {
            return false;
        }

        try {
            new URL(input).toURI();
            return Patterns.WEB_URL.matcher(input).matches();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getUniqueFileName(DocumentFile documentFolder, String fileName, boolean tryActualFile) {
        if (tryActualFile && documentFolder.findFile(fileName) == null){
            return fileName;
        }

        int pathStartPosition = fileName.lastIndexOf(".");

        String mergedName = pathStartPosition != -1 ? fileName.substring(0, pathStartPosition) : fileName;
        String fileExtension = pathStartPosition != -1 ? fileName.substring(pathStartPosition) : "";

        if (mergedName.length() == 0 && fileExtension.length() > 0) {
            mergedName = fileExtension;
            fileExtension = "";
        }

        for (int exceed = 1; exceed < 9999; exceed++) {
            String newName = mergedName + " (" + exceed + ")" + fileExtension;

            if (documentFolder.findFile(newName) == null)
                return newName;
        }

        return fileName;
    }

    @NotNull
    public static DocumentFile getUploadDocumentFile() {
        String defaultPath = getUploadDir();
        File defaultFolder = new File(defaultPath);

        if (!defaultFolder.exists()) {
            System.out.println(defaultFolder.mkdirs());
        }

        return DocumentFile.fromFile(defaultFolder);
    }

    @NotNull
    public static DocumentFile getDownloadDocumentFile() {
        String defaultPath = getDownloadDir();
        File defaultFolder = new File(defaultPath);

        if (!defaultFolder.exists()) {
            System.out.println(defaultFolder.mkdirs());
        }

        return DocumentFile.fromFile(defaultFolder);
    }

    @NotNull
    public static String getDownloadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/FileTranseferDemo/download";
    }

    @NotNull
    public static String getUploadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/FileTranseferDemo/upload";
    }

    @NotNull
    public static IntentFilter configWifiStageFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
       /* intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);*/
        return intentFilter;
    }

    public static WifiManager getWifiManager(@NotNull Context context) {
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static WifiConfiguration getNetworkConfig(WifiManager wifiManager) {
        if (wifiManager == null) {
            return null;
        }
        Method[] methods = WifiManager.class.getDeclaredMethods();
        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "getWifiApConfiguration")) {
                try {
                    return (WifiConfiguration) method.invoke(wifiManager);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static int findFreePort() {
        for (int i = DEFAULT_WEB_SERVER_PORT; i <= MAX_PORT_NUMBER; i++) {
            if (isAvailablePort(i)) {
                return i;
            }
        }
        throw new RuntimeException("Could not find an isAvailable port between " + DEFAULT_WEB_SERVER_PORT + " and " + MAX_PORT_NUMBER);
    }

    private static boolean isAvailablePort(final int port) {
        ServerSocket serverSocket = null;
        DatagramSocket dataSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            dataSocket = new DatagramSocket(port);
            dataSocket.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (dataSocket != null) {
                dataSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Nullable
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public static Bitmap generateQr(int size, String inputText) throws WriterException {
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        BitMatrix bitMatrix = multiFormatWriter.encode(inputText, BarcodeFormat.QR_CODE, size, size, hints);
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        return barcodeEncoder.createBitmap(bitMatrix);
    }

}
