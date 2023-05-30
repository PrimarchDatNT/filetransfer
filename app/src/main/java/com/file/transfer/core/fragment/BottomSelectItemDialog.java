package com.file.transfer.core.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.file.transfer.R;
import com.file.transfer.core.model.BrowserType;
import com.file.transfer.core.model.ListItem;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.DensityUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.DialogBottomSelectItemBinding;
import com.file.transfer.databinding.ItemTransferFileListBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BottomSelectItemDialog extends BottomSheetDialogFragment {

    private static final String TAG = "bottom_select_items";

    private final Callback callback;

    private Context context;
    private ItemAdapter adapter;
    private List<String> listItem;
    private DialogBottomSelectItemBinding binding;

    public BottomSelectItemDialog(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public int getTheme() {
        return R.style.BottomDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogBottomSelectItemBinding.inflate(inflater, container, false);
        float pixels = (float) DensityUtil.heightPixels(this.context);
        int configSize = (int) ((4 * pixels) / 9f);
        ViewGroup.LayoutParams layoutParams = this.binding.rvSelectedItems.getLayoutParams();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = configSize;
        this.binding.rvSelectedItems.setLayoutParams(layoutParams);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.updateTotalSelect();

        this.adapter = new ItemAdapter();
        this.binding.rvSelectedItems.setAdapter(this.adapter);
        this.binding.rvSelectedItems.setLayoutManager(new LinearLayoutManager(this.context));

        this.binding.ivMore.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                dismiss();
            }
        });

        this.binding.tvCofirmSend.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                dismiss();

                if (callback != null) {
                    callback.onConfrimSend();
                }
            }
        });
    }

    private void updateTotalSelect() {
        if (this.listItem == null || this.listItem.isEmpty()) {
            this.binding.tvSendInfo.setText(R.string.empty_select_file_send);
            return;
        }

        long size = 0;
        for (String path : this.listItem) {
            File file = new File(path);
            size += file.exists() ? file.length() : 0;
        }

        this.binding.tvSendInfo.setText(this.getString(R.string.send_file_info, this.listItem.size(), AppUtil.convertBytes(size)));
    }

    public void showEx(@NonNull FragmentManager manager, List<String> listItem) {
        try {
            this.listItem = new ArrayList<>(listItem);

            try {
                FragmentTransaction ft = manager.beginTransaction();
                Fragment bottomFragment = manager.findFragmentByTag(TAG);
                if (bottomFragment != null && bottomFragment.isAdded()) {
                    ft.remove(bottomFragment).commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            super.show(manager, TAG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (this.callback != null) {
            this.callback.onCloseMore();
        }
    }

    private void removeItem(int position) {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return;
        }

        this.listItem.remove(position);

        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }

        if (this.callback != null) {
            this.callback.onRemoveItem(position);
        }

        if (this.listItem.isEmpty()) {
            this.dismiss();
        } else {
            this.updateTotalSelect();
        }
    }

    public interface Callback {

        void onConfrimSend();

        void onRemoveItem(int position);

        void onCloseMore();
    }

    private final class ItemHolder extends RecyclerView.ViewHolder {

        private final ItemTransferFileListBinding binding;

        public ItemHolder(@NonNull ItemTransferFileListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindItem(int position) {
            if (listItem == null || listItem.isEmpty()) {
                return;
            }

            String path = listItem.get(position);
            if (TextUtils.isEmpty(path)) {
                return;
            }

            ListItem file = new ListItem(path);
            if (!file.exists()) {
                return;
            }
            file.setType(AppUtil.getMimeTypeByName(file.getName()));

            int resId = AppUtil.getMineTypeIcon(file);

            if (file.getType() == BrowserType.IMAGE || file.getType() == BrowserType.VIDEO) {
                Glide.with(context)
                        .load(file.getPath())
                        .override(65)
                        .error(file.getType() == BrowserType.IMAGE ? R.drawable.ic_file_image : R.drawable.ic_file_video)
                        .into(this.binding.ivAvatarThumb);

                this.binding.ivAvatarThumb.setBackgroundResource(R.drawable.bg_image_border);

            } else if (file.getType() == BrowserType.APK) {
                Drawable apkIcon = AppUtil.getApkIcon(context, file.getPath());
                if (apkIcon != null) {
                    this.binding.ivAvatarThumb.setImageDrawable(apkIcon);
                } else {
                    this.binding.ivAvatarThumb.setImageResource(R.drawable.ic_file_apk);
                }

            } else {
                this.binding.ivAvatarThumb.setImageResource(resId);
                this.binding.ivAvatarThumb.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            }

            this.binding.tvTitle.setText(file.getName());
            this.binding.tvFileInfo.setVisibility(View.GONE);
            this.binding.ivSelectStage.setImageResource(R.drawable.ic_transfer_close_select);

            this.binding.ivSelectStage.setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    removeItem(position);
                }
            });
        }
    }

    private final class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ItemHolder(ItemTransferFileListBinding.inflate(LayoutInflater.from(context), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ItemHolder) {
                ((ItemHolder) holder).bindItem(position);
            }
        }

        @Override
        public int getItemCount() {
            return listItem == null ? 0 : listItem.size();
        }
    }
}
