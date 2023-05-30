package com.file.transfer.core.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.FragmentTransferOptionBinding;

import org.jetbrains.annotations.NotNull;

public class TransferOptionFragment extends Fragment {

    private Callback callback;
    private FragmentTransferOptionBinding mBinding;

    public TransferOptionFragment() {
    }

    @NotNull
    public static TransferOptionFragment newInstance(Callback callback) {
        TransferOptionFragment fragment = new TransferOptionFragment();
        fragment.callback = callback;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mBinding = FragmentTransferOptionBinding.inflate(inflater, container, false);
        return this.mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.mBinding.llFeatureShareWifi.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onShareWifi();
                }
            }
        });

        this.mBinding.llFeatureShareHotspot.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onShareHotspot();
                }
            }
        });

        this.mBinding.tvInputAddress.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onInputAddress();
                }
            }
        });

        this.mBinding.ivScanQr.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onScan();
                }
            }
        });
    }

    public interface Callback {

        void onShareWifi();

        void onShareHotspot();

        void onInputAddress();

        void onScan();
    }
}