package com.file.transfer.core.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.file.transfer.R;
import com.file.transfer.databinding.ItemTransferConfigBinding;

import java.util.ArrayList;
import java.util.List;

public class BottomConfigAdapter extends RecyclerView.Adapter<BottomConfigAdapter.ViewHolder> {

    private final Context context;
    private List<String> listData;
    private String currItem;
    private OnItemConfigListener itemConfigListener;

    public BottomConfigAdapter(Context context) {
        this.context = context;
    }

    public void setCurrItem(String currItem) {
        this.currItem = currItem;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListData(List<String> data) {
        if (this.listData == null) {
            this.listData = new ArrayList<>();
        }
        this.listData.clear();
        if (data != null) {
            this.listData.addAll(data);
        }
        this.notifyDataSetChanged();
    }

    public void setItemConfigListener(OnItemConfigListener itemConfigListener) {
        this.itemConfigListener = itemConfigListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTransferConfigBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (this.listData == null) {
            return;
        }

        String name = this.listData.get(position);
        if (TextUtils.isEmpty(name)) {
            return;
        }

        holder.binding.clProfile.setVisibility(View.VISIBLE);
        holder.binding.tvName.setText(name);
        holder.binding.tvName.setTextColor(ContextCompat.getColor(context,
                TextUtils.equals(name, this.currItem) ? R.color.colorPrimary : R.color.label_prim));
        holder.binding.clContainer.setOnClickListener(view -> {
            if (this.itemConfigListener != null) {
                this.itemConfigListener.onClick(name, position);
            }
        });

        holder.binding.ivDelete.setOnClickListener(view -> {
            if (this.itemConfigListener != null) {
                this.itemConfigListener.onDelete(name, position);
            }
            this.listData.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        if (this.listData == null) return 0;
        return this.listData.size();
    }

    public interface OnItemConfigListener {

        void onClick(String name, int position);

        void onDelete(String name, int position);
    }

    public static class ViewHolder extends BaseViewHolderBinding<ItemTransferConfigBinding> {

        public ViewHolder(ItemTransferConfigBinding binding) {
            super(binding);
        }
    }
}
