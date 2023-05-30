package com.file.transfer;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.file.zip.ZipAcitvity;

public class HomeAcitvity extends AppCompatActivity {


    private static final int REQ_FILE_MANAGER_ACCESS_CODE = 1593;
    private static final int REQ_STORAGE_PERMISSION_CODE = 1489;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_acitvity);

        findViewById(R.id.btnAction).setOnClickListener(v -> this.startActivity(new Intent(this, ZipAcitvity.class)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            this.reqStoreMananger();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.isNotStoragePmsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, REQ_STORAGE_PERMISSION_CODE);
                return;
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void reqStoreMananger() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        this.startActivityForResult(intent, REQ_FILE_MANAGER_ACCESS_CODE);
    }

    private boolean isNotStoragePmsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }


}