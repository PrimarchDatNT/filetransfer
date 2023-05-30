package com.file.transfer.core;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.file.transfer.R;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.ItemTransferPathFileBinding;

import java.io.File;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final Callback callback;

    private List<String> rootPaths;

    public FolderAdapter(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void setRootPaths(List<String> rootPaths) {
        this.rootPaths = rootPaths;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemHolder(ItemTransferPathFileBinding.inflate(LayoutInflater.from(this.context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemHolder) {
            ((ItemHolder) holder).bindItem(position);
        }
    }

    @Override
    public int getItemCount() {
        return this.rootPaths == null ? 0 : this.rootPaths.size();
    }

    public interface Callback {

        void onClickFolderItem(int position);
    }

    private final class ItemHolder extends RecyclerView.ViewHolder {

        private final ItemTransferPathFileBinding binding;

        public ItemHolder(@NonNull ItemTransferPathFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindItem(int position) {
            String path = rootPaths.get(position);
            if (TextUtils.isEmpty(path)) {
                return;
            }

            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                return;
            }

            if (TextUtils.equals(path, Environment.getExternalStorageDirectory().getAbsolutePath())) {
                this.binding.tvPathItem.setText(R.string.intenal_storage_title);

            } else if (position == 0) {
                this.binding.tvPathItem.setText(R.string.sd_card_title);

            } else {
                this.binding.tvPathItem.setText(file.getName());
            }

            this.binding.clBackground.setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    if (callback != null) {
                        callback.onClickFolderItem(position);
                    }
                }
            });
        }
    }
}
