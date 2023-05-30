package com.file.transfer.core.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.file.transfer.R;
import com.file.transfer.core.ConnectUtils;
import com.file.transfer.core.util.DensityUtil;
import com.file.transfer.databinding.FragmentConectionBinding;
import com.google.zxing.WriterException;

import org.jetbrains.annotations.NotNull;

public class ConectionFragment extends Fragment {

    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";

    private Context context;
    private String severAdress;
    private String sharePw;
    private String hotspotName;

    private FragmentConectionBinding mBinding;

    public ConectionFragment() {
    }

    public static ConectionFragment newInstance(String hotspotName, String sharePw) {
        ConectionFragment fragment = new ConectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM2, hotspotName);
        args.putString(ARG_PARAM3, sharePw);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = this.getArguments();
        if (arguments != null) {
            this.hotspotName = arguments.getString(ARG_PARAM2);
            this.sharePw = arguments.getString(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mBinding = FragmentConectionBinding.inflate(inflater, container, false);
        return this.mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean isShareWifi = TextUtils.isEmpty(this.hotspotName);
        this.mBinding.rlHotspotName.setVisibility(isShareWifi ? View.GONE : View.VISIBLE);
        this.mBinding.rlHotspotSharePw.setVisibility(TextUtils.isEmpty(this.sharePw) ? View.GONE : View.VISIBLE);
        this.mBinding.tvStep1Title.setText(isShareWifi ? R.string.step1_title_wifi : R.string.step1_title_hotspot);
        this.mBinding.tvStep1Message.setText(isShareWifi ? R.string.step1_message_wifi : R.string.step1_message_hotspot);
        this.mBinding.tvHotspotName.setText(this.hotspotName == null ? "" : this.hotspotName);
        this.mBinding.tvHotspotPassword.setText(this.sharePw == null ? "" : this.sharePw);
        this.showErrorLayout(true);
    }

    public void updateHotspotStage(String hotspotName, String sharePw) {
        this.hotspotName = hotspotName;
        this.sharePw = sharePw;
        this.mBinding.tvHotspotName.setText(this.hotspotName == null ? "" : this.hotspotName);
        this.mBinding.tvHotspotPassword.setText(this.sharePw == null ? "" : this.sharePw);
    }

    public void updateAddressStage(String severAdress) {
        this.severAdress = severAdress;
        this.showAddress();
    }

    public void showErrorLayout(boolean isError) {
        this.mBinding.flQrFrame.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
        this.mBinding.clStep2.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
        this.mBinding.pbWaitNetwork.setVisibility(isError ? View.VISIBLE : View.GONE);
    }

    private void showAddress() {
        if (TextUtils.isEmpty(this.severAdress)) {
            this.showErrorLayout(true);
            return;
        }

        this.showErrorLayout(false);
        this.mBinding.tvServerAddress.setText(this.severAdress);

        try {
            int size = DensityUtil.widthPixels(this.context) / 2;

            if (!TextUtils.isEmpty(this.hotspotName) && !TextUtils.isEmpty(this.sharePw)) {
                String text = String.format(this.requireContext().getString(R.string.share_hotspot_qr), this.hotspotName, this.sharePw, this.severAdress);
                Bitmap bitmap = ConnectUtils.generateQr(size, text);

                if (bitmap != null) {
                    Glide.with(this.context)
                            .load(bitmap)
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners((int) this.context.getResources().getDimension(R.dimen.px18))))
                            .into(this.mBinding.ivServerAddress);
                }
                return;
            }

            Bitmap bitmap = ConnectUtils.generateQr(size, this.severAdress);

            if (bitmap != null) {
                Glide.with(this.context)
                        .load(bitmap)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners((int) this.context.getResources().getDimension(R.dimen.px18))))
                        .into(this.mBinding.ivServerAddress);
            }

        } catch (WriterException e) {
            e.printStackTrace();
            this.mBinding.ivServerAddress.setVisibility(View.GONE);
        }
    }

}