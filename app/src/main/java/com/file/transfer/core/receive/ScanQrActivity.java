package com.file.transfer.core.receive;

import static android.Manifest.permission.CAMERA;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.file.transfer.R;
import com.file.transfer.core.ConnectUtils;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.DensityUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.ActivityScanQrBinding;
import com.file.transfer.databinding.DialogRenameLibararyBinding;
import com.google.zxing.Result;

import org.jetbrains.annotations.NotNull;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanQrActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int REQ_CAMERA_CODE = 3559;
    private static final float DEFAULT_SQUARE_DIMENSION_RATIO = 5f / 8;

    private static final String[] PERMISSIONS_CAMERA = {Manifest.permission.CAMERA};

    private boolean activeFlash;
    private boolean isStartSettingPermission;

    private ZXingScannerView scannerView;
    private ActivityScanQrBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.mBinding = ActivityScanQrBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.mBinding.getRoot());
        this.scannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                CustomViewFinderView viewFinderView = new CustomViewFinderView(context);
                viewFinderView.setLaserEnabled(true);
                return viewFinderView;
            }
        };

        this.scannerView.setResultHandler(this);
        this.mBinding.flCameraView.addView(this.scannerView);
        this.mBinding.ivFlash.setSelected(false);
        this.initCenter(this.mBinding.squareCenter);
    }

    private void initCenter(@NotNull View view) {
        int pixels = DensityUtil.widthPixels(this);
        int configSize = (int) (pixels * DEFAULT_SQUARE_DIMENSION_RATIO);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = configSize;
        layoutParams.height = configSize;
        view.setLayoutParams(layoutParams);
    }

    public void onClickInputScan(View view) {
        this.offFlashForInput();
        this.stopScan();

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
                AppUtil.hideKeyboard(ScanQrActivity.this, binding.edFileRename);
                dialog.dismiss();
                startScan();
            }
        });

        binding.tvConfirmRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                Editable text = binding.edFileRename.getText();
                if (text == null || TextUtils.isEmpty(text.toString().trim())) {
                    Toast.makeText(ScanQrActivity.this, R.string.empty_input_address_msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                AppUtil.hideKeyboard(ScanQrActivity.this, binding.edFileRename);
                dialog.dismiss();
                finishScan(text.toString());
            }
        });

        AppUtil.showKeyboard(this);
        AppUtil.showFullScreenDialog(dialog);
    }

    public void onClickFlash(View view) {
        if (!this.isSupportFlash()) {
            Toast.makeText(this, R.string.err_flash_support_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        this.activeFlash = !this.activeFlash;
        this.toggleFlash(this.activeFlash);
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }

    private void offFlashForInput() {
        if (this.activeFlash) {
            this.activeFlash = false;
            this.toggleFlash(false);
        }
    }

    private void toggleFlash(boolean activeFlash) {
        this.mBinding.ivFlash.setSelected(activeFlash);

        if (this.scannerView != null) {
            this.scannerView.toggleFlash();
        }
    }

    private void finishScan(String result) {
        Intent intent = new Intent();
        intent.putExtra(ConnectUtils.EXTRA_SCAN_RESULT, result);
        this.setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    private void requestPmsForScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.isNoCameraPms()) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, REQ_CAMERA_CODE);
                return;
            }
        }
        this.scannerView.startCamera();
    }

    private boolean isSupportFlash() {
        return this.getPackageManager().hasSystemFeature("android.hardware.camera.flash");
    }

    private boolean isNoCameraPms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    public void handleResult(@NotNull Result rawResult) {
        this.stopScan();

        String text = rawResult.getText();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, R.string.scan_err_msg, Toast.LENGTH_SHORT).show();
            this.startScan();
            return;
        }

        this.showResultDialog(text);
    }

    private void showResultDialog(@NotNull String result) {
        this.offFlashForInput();
        if (result.startsWith("WIFI:")) {
            finishScan(result);
            return;
        }

        DialogRenameLibararyBinding binding = DialogRenameLibararyBinding.inflate(this.getLayoutInflater());
        Dialog dialog = AppUtil.createDialog(this);
        dialog.setContentView(binding.getRoot());
        binding.tvDialogCancelCompressTitle.setText(R.string.scan_result_title);
        binding.tvConfirmRename.setText(R.string.cta_connect);
        binding.edFileRename.setText(result);
        binding.edFileRename.setSelection(result.length());

        binding.tvCancelRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                dialog.dismiss();
                startScan();
            }
        });

        binding.tvConfirmRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                Editable text = binding.edFileRename.getText();
                if (text == null || TextUtils.isEmpty(text)) {
                    Toast.makeText(ScanQrActivity.this, R.string.empty_input_address_msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                finishScan(text.toString());
            }
        });

        dialog.show();
    }

    private void showDialogPermission() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_permission)
                .setMessage(R.string.camera_pms_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.go_setting, (dialog, which) -> {
                    dialog.dismiss();
                    isStartSettingPermission = true;
                    goSetting();
                })
                .create()
                .show();
    }

    private void goSetting() {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + this.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        this.startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.stopScan();
    }

    private void startScan() {
        if (this.scannerView != null) {
            this.scannerView.setResultHandler(this);
            this.scannerView.startCamera();
        }
    }

    private void stopScan() {
        if (this.scannerView != null) {
            this.scannerView.stopCameraPreview();
            this.scannerView.stopCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.isStartSettingPermission) {
            if (this.isNoCameraPms()) {
                this.finish();
                return;
            }

            this.startScan();

            return;
        }

        this.requestPmsForScan();
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_CODE) {
            SharedPreferences preferences = this.getSharedPreferences("rar-extractor-prefs", Context.MODE_PRIVATE);
            for (String permission : permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                        if (permission.equals(CAMERA)) {
                            this.startScan();
                            return;
                        }
                    } else if (permission.equals(CAMERA)) {
                        preferences.edit().putBoolean("k_dont_ask_camera", true).apply();
                    }
                }
            }

            if (preferences.getBoolean("k_dont_ask_camera", false)) {
                this.showDialogPermission();
            } else {
                this.finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        System.gc();
        super.onDestroy();
    }
}