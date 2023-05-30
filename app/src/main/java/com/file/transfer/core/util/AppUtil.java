package com.file.transfer.core.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.file.transfer.R;
import com.file.transfer.core.model.BrowserType;
import com.file.transfer.core.model.ListItem;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.HashMap;

public class AppUtil {

    public static final char EXTENSION_SEPARATOR = '.';

    public static final String[] ARC_NAME_FILE_RAR_TYPE = {"r##", "jar", "7z", "gz", "tgz", "bz2", "bz", "tbz", "tbz2", "xz", "txz", "lz", "tlz", "tar", "iso", "lzh", "lha", "arj", "a##", "z", "taz", "001"};
    public static final String[] ARC_NAME_FILE_ZIP_TYPE = {"zip", "zipx", "z##"};

    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';


    @NonNull
    public static Dialog createDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.create();
        return dialog;
    }

    public static void showFullScreenDialog(Dialog dialog) {
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        dialog.show();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.CENTER);
    }

    @NotNull
    public static HashMap<String, Integer> getMapFormat() {
        HashMap<String, Integer> mapFormat = new HashMap<>();
        mapFormat.put("pdf", BrowserType.PDF);
        mapFormat.put("txt", BrowserType.TXT);
        mapFormat.put("apk", BrowserType.APK);
        mapFormat.put("aac", BrowserType.AUDIO);
        mapFormat.put("mp3", BrowserType.AUDIO);
        mapFormat.put("amr", BrowserType.AUDIO);
        mapFormat.put("mpg", BrowserType.AUDIO);
        mapFormat.put("wav", BrowserType.AUDIO);
        mapFormat.put("m4a", BrowserType.AUDIO);
        mapFormat.put("wma", BrowserType.AUDIO);
        mapFormat.put("ogg", BrowserType.AUDIO);
        mapFormat.put("flac", BrowserType.AUDIO);
        mapFormat.put("3gp", BrowserType.VIDEO);
        mapFormat.put("mp4", BrowserType.VIDEO);
        mapFormat.put("mkv", BrowserType.VIDEO);
        mapFormat.put("webm", BrowserType.VIDEO);
        mapFormat.put("wmv", BrowserType.VIDEO);
        mapFormat.put("3g2", BrowserType.VIDEO);
        mapFormat.put("rar", BrowserType.RAR);

        for (String extension : ARC_NAME_FILE_RAR_TYPE) {
            mapFormat.put(extension, BrowserType.ARCHIVE);
        }

        for (String extension : ARC_NAME_FILE_ZIP_TYPE) {
            mapFormat.put(extension, BrowserType.ZIP);
        }

        mapFormat.put("jpg", BrowserType.IMAGE);
        mapFormat.put("png", BrowserType.IMAGE);
        mapFormat.put("bmp", BrowserType.IMAGE);
        mapFormat.put("gif", BrowserType.IMAGE);
        mapFormat.put("jpeg", BrowserType.IMAGE);
        mapFormat.put("webp", BrowserType.IMAGE);
        mapFormat.put("xls", BrowserType.EXCEL);
        mapFormat.put("xlsx", BrowserType.EXCEL);
        mapFormat.put("doc", BrowserType.WORD);
        mapFormat.put("docx", BrowserType.WORD);
        mapFormat.put("ppt", BrowserType.PTT);
        mapFormat.put("pptx", BrowserType.PTT);
        return mapFormat;
    }

    public static int getMineTypeIcon(ListItem item) {
        if (item == null) {
            return R.drawable.ic_file_unknow;
        }
        return getMineTypeIcon(item.type);
    }

    public static Drawable getApkIcon(@NotNull Context context, String path) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                try {
                    ApplicationInfo appInfo = packageInfo.applicationInfo;
                    appInfo.sourceDir = path;
                    appInfo.publicSourceDir = path;
                    return appInfo.loadIcon(context.getPackageManager());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ContextCompat.getDrawable(context, R.drawable.ic_file_apk);
    }

    public static int getMineTypeIcon(String name) {
        if (name == null || TextUtils.isEmpty(name)) {
            return R.drawable.ic_file_unknow;
        }
        int type = getMimeTypeByName(name);
        return getMineTypeIcon(type);
    }

    public static int getMimeTypeByName(String name) {
        int type = BrowserType.OTHER;
        String extension = AppUtil.getExtension(name);
        HashMap<String, Integer> mapFormat = getMapFormat();
        if (!TextUtils.isEmpty(extension) && mapFormat.containsKey(extension)) {
            Integer integer = mapFormat.get(extension);
            if (integer != null) {
                type = integer;
            }
        }
        return type;
    }

    public static int getMineTypeIcon(int type) {
        int resId = R.drawable.ic_file_unknow;

        if (type == BrowserType.VIDEO) {
            resId = R.drawable.ic_file_video;
        }

        if (type == BrowserType.IMAGE) {
            resId = R.drawable.ic_file_image;
        }

        if (type == BrowserType.CLOUD) {
            resId = R.drawable.ic_file_cloud;
        }

        if (type == BrowserType.TXT) {
            resId = R.drawable.ic_file_text;
        }

        if (type == BrowserType.PDF) {
            resId = R.drawable.ic_file_pdf;
        }

        if (type == BrowserType.WORD) {
            resId = R.drawable.ic_file_doc;
        }

        if (type == BrowserType.EXCEL) {
            resId = R.drawable.ic_file_excel;
        }

        if (type == BrowserType.PTT) {
            resId = R.drawable.ic_file_powerpoint;
        }

        if (type == BrowserType.AUDIO) {
            resId = R.drawable.ic_file_audio;
        }

        if (type == BrowserType.APK) {
            resId = R.drawable.ic_file_apk;
        }

        if (type == BrowserType.ZIP) {
            resId = R.drawable.ic_file_zip;
        }

        if (type == BrowserType.RAR) {
            resId = R.drawable.ic_file_rar;
        }

        if (type == BrowserType.ARCHIVE) {
            resId = R.drawable.ic_file_archive;
        }

        if (type == BrowserType.DIRECTORY || type == BrowserType.BOOKMARK) {
            resId = R.drawable.ic_folder;
        }

        return resId;
    }

    public static void showKeyboard(@NonNull Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void hideKeyboard(Context context, View anchorView) {
        try {
            Activity activity = (Activity) context;
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(anchorView.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (cm != null) {
            info = cm.getActiveNetworkInfo();
        }
        return info != null && info.isConnected();
    }


    @NotNull
    public static String getExtension(String filename) {
        String ext;
        final int index = indexOfExtension(filename);
        if (filename == null) {
            ext = null;
        } else if (index == -1) {
            ext = "";
        } else {
            ext = filename.substring(index + 1);
        }
        if (!isNullOrEmpty(ext)) {
            return ext.toLowerCase();
        }
        return "";
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static int indexOfExtension(final String filename) {
        if (filename == null) {
            return -1;
        }

        final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }

    public static int indexOfLastSeparator(final String filename) {
        if (filename == null) {
            return -1;
        }

        final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    @NotNull
    public static String convertBytes(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
