package com.file.transfer.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static boolean checkSeftSDPermission(@NotNull Context context, List<String> listItemSelected) {
        String extCardPath = getExtCardPath(context, false);
        boolean isCheckPermission = false;
        boolean fileSdcard = false;
        if (!TextUtils.isEmpty(extCardPath)) {
            for (String path : listItemSelected) {
                if (!TextUtils.isEmpty(path) && path.contains(extCardPath)) {
                    fileSdcard = true;
                    break;
                }
            }

            if (fileSdcard) {
                DocumentFile documentDirectory = getAvailabeAccessDocumentDirectory(context, extCardPath);
                if (documentDirectory == null) {
                    isCheckPermission = true;
                }
            }
        }
        return isCheckPermission;
    }

    public static boolean checkSeftSDPermission(@NotNull Context context, String itemSelected) {
        String extCardPath = getExtCardPath(context, false);
        boolean isCheckPermission = false;
        boolean fileSdcard = false;
        if (!TextUtils.isEmpty(extCardPath)) {
            if (!TextUtils.isEmpty(itemSelected) && itemSelected.contains(extCardPath)) {
                fileSdcard = true;
            }

            if (fileSdcard) {
                DocumentFile documentDirectory = getAvailabeAccessDocumentDirectory(context, extCardPath);
                if (documentDirectory == null) {
                    isCheckPermission = true;
                }
            }
        }
        return isCheckPermission;
    }

    public static void requestSDPermission(@NotNull Activity activity, int requestCode) {
        Intent intent;
        String extCardPath = getExtCardPath(activity, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StorageManager storageManager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getStorageVolume(new File(extCardPath));
            intent = storageVolume.createOpenDocumentTreeIntent();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StorageManager storageManager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getStorageVolume(new File(extCardPath));
            intent = storageVolume.createAccessIntent(null);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        }

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getExtCardPath(Context context, boolean isFileDir) {
        ArrayList<ExDirItem> dirList = getDirList(context);
        if (dirList.size() < 2) {
            return "";
        }
        return isFileDir ? dirList.get(1).fileDir : dirList.get(1).rootDir;
    }

    public static ArrayList<ExDirItem> getDirList(Context context) {
        String str;
        ArrayList<ExDirItem> arrayList = new ArrayList<>();

        File[] fileArr = null;
        try {
            fileArr = context.getExternalFilesDirs(null);
        } catch (NullPointerException unused) {
            unused.printStackTrace();
        }

        if (fileArr == null) {
            return arrayList;
        }

        for (int i = 0; i < fileArr.length; i++) {
            try {
                File file = fileArr[i];
                if (file != null) {
                    str = Environment.getExternalStorageState(file);
                    if (str.equals("mounted") || str.equals("mounted_ro") || str.equals("shared")) {

                        ExDirItem exDirItem = new ExDirItem();
                        String absolutePath = file.getAbsolutePath();
                        exDirItem.fileDir = absolutePath;
                        exDirItem.usb = false;

                        if (Build.VERSION.SDK_INT >= 24) {
                            exDirItem.usb = context.getSystemService(StorageManager.class).getStorageVolume(new File(exDirItem.fileDir)).getDescription(context).toUpperCase().contains("USB");
                        }

                        exDirItem.intCard = i == 0;
                        int indexOf = absolutePath.indexOf("/Android/");

                        if (indexOf != -1) {
                            exDirItem.rootDir = absolutePath.substring(0, indexOf);
                            arrayList.add(exDirItem);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    public static @Nullable
    DocumentFile getAvailabeAccessDocumentDirectory(@NotNull Context context, String path) {
        List<UriPermission> uriPermissions = context.getApplicationContext().getContentResolver().getPersistedUriPermissions();
        if (uriPermissions == null || uriPermissions.isEmpty()) {
            return null;
        }

        for (UriPermission uriPermission : uriPermissions) {
            return getAccessDocumentFile(context, uriPermission.getUri(), path);
        }
        return null;
    }

    public static @Nullable
    DocumentFile getAccessDocumentFile(Context context, Uri rootDir, String path) {
        File file = new File(path);
        if (file.exists()) {
            DocumentFile diretory = DocumentFile.fromTreeUri(context, rootDir);
            if (diretory == null || TextUtils.isEmpty(diretory.getName())) {
                return null;
            }

            takeUriPermssion(context, rootDir);
            return diretory;
        }
        return null;
    }

    public static void takeUriPermssion(@NotNull Context context, Uri rootDir) {
        try {
            if (rootDir != null) {
                int modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                context.getApplicationContext().getContentResolver().takePersistableUriPermission(rootDir, modeFlags);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
