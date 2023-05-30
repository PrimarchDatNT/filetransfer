package com.file.transfer.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.file.transfer.R;
import com.file.transfer.core.model.BrowserType;
import com.file.transfer.core.model.ListItem;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.ItemTransferFileListBinding;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SelectFileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;

    private Callback callback;
    private List<ListItem> listItem;
    private final boolean isSelectOnlyFile;

    public SelectFileAdapter(Context context, boolean isSelectOnlyFile) {
        this.context = context;
        this.isSelectOnlyFile = isSelectOnlyFile;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public ListItem getItemPosition(int position) {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return null;
        }
        return this.listItem.get(position);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListItem(List<ListItem> listItem) {
        this.listItem = listItem;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemHolder(ItemTransferFileListBinding.inflate(LayoutInflater.from(this.context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemHolder) {
            ((ItemHolder) holder).bindItem(position);
        }
    }

    @Override
    public int getItemCount() {
        return this.listItem == null ? 0 : this.listItem.size();
    }

    public interface Callback {

        void onClickItem(int position);

        void onClickCheckItem(int position);
    }

    private final class ItemHolder extends RecyclerView.ViewHolder {

        private final ItemTransferFileListBinding binding;

        public ItemHolder(@NonNull ItemTransferFileListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void bindItem(int position) {
            ListItem item = listItem.get(position);
            if (item == null) {
                return;
            }

            int resId = AppUtil.getMineTypeIcon(item);

            if (item.getType() == BrowserType.IMAGE || item.getType() == BrowserType.VIDEO) {
                Glide.with(context)
                        .load(item.getPath())
                        .override(65)
                        .error(item.getType() == BrowserType.IMAGE ? R.drawable.ic_file_image : R.drawable.ic_file_video)
                        .into(this.binding.ivAvatarThumb);

                this.binding.ivAvatarThumb.setBackgroundResource(R.drawable.bg_image_border);

            } else if (item.getType() == BrowserType.APK) {
                Drawable apkIcon = AppUtil.getApkIcon(context, item.getPath());
                if (apkIcon != null) {
                    this.binding.ivAvatarThumb.setImageDrawable(apkIcon);
                } else {
                    this.binding.ivAvatarThumb.setImageResource(R.drawable.ic_file_apk);
                }

            } else {
                this.binding.ivAvatarThumb.setImageResource(resId);
                this.binding.ivAvatarThumb.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            }

            this.binding.tvTitle.setText(item.getName());
            int count = 0;
            if (item.getType() == BrowserType.DIRECTORY) {
                try {
                    File[] files = new File(item.path).listFiles();
                    if (files != null) {
                        count = files.length;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String size = item.getType() == BrowserType.DIRECTORY ? context.getString(R.string.folder_item_count, count)
                    : AppUtil.convertBytes(item.getSize());
            this.binding.tvFileInfo.setText(size + "  |  " + formatDate(item.mtime));

            int isShowCheckbox = (isSelectOnlyFile || item.getType() == BrowserType.DIRECTORY) ? View.GONE : View.VISIBLE;
            this.binding.ivSelectStage.setVisibility(isShowCheckbox);
            if (!isSelectOnlyFile) {
                this.binding.ivSelectStage.setSelected(item.isSelected());
            }

            this.binding.ivSelectStage.setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    if (callback != null) {
                        callback.onClickCheckItem(position);
                    }
                }
            });

            this.binding.getRoot().setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    if (callback != null) {
                        if (item.getType() == BrowserType.DIRECTORY) {
                            callback.onClickItem(position);
                        } else {
                            callback.onClickCheckItem(position);
                        }
                    }
                }
            });

        }

        public String formatDate(long millis) {
            @SuppressLint("SimpleDateFormat") DateFormat formater = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            return formater.format(new Date(millis));
        }
    }

}
