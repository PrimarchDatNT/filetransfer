package com.file.transfer.core;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.file.transfer.R;
import com.file.transfer.core.model.BrowserType;
import com.file.transfer.core.model.ListItem;
import com.file.transfer.core.model.TransferModel;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.ItemTransferHeaderBinding;
import com.file.transfer.databinding.ItemTransferObjectBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferManagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_HIDDEN = 1;
    private static final int ITEM_NORMAL = 3;
    private static final int ITEM_HEADER = 5;

    private final boolean onManager;
    private final Context mContext;
    private final HeaderItem firstHeader;
    private final HeaderItem secondHeader;

    private boolean expandSendItem;
    private boolean expandReciveItem;

    private OnClickItemListener listener;

    private List<Object> listItem;
    private List<TransferModel> listFirsItem;
    private List<TransferModel> listSecondItem;

    public TransferManagerAdapter(Context context, boolean onManager) {
        this.mContext = context;
        this.onManager = onManager;
        this.firstHeader = new HeaderItem();
        this.secondHeader = new HeaderItem();
        this.listItem = new ArrayList<>();
        this.listItem.add(this.firstHeader);
        this.listItem.add(this.secondHeader);
    }

    public Object getItemPosition(int position) {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return null;
        }
        return this.listItem.get(position);
    }

    public int getPositionByPath(String path) {
        if (TextUtils.isEmpty(path) || this.listItem == null || this.listItem.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < this.listItem.size(); i++) {
            Object item = this.listItem.get(i);
            if (item instanceof TransferModel) {
                if (TextUtils.equals(path, ((TransferModel) item).realPaths)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public void setListener(OnClickItemListener listener) {
        this.listener = listener;
    }

    public void setFirstItems(List<TransferModel> listItem) {
        int size = this.listItem.size();

        if (this.listFirsItem == null || this.listFirsItem.isEmpty()) {
            size += listItem.size();
        } else {
            size = size - this.listFirsItem.size() + listItem.size();
        }

        Object[] arrItem = new Object[size];
        this.listFirsItem = listItem;

        for (int i = 0; i < size; i++) {
            if (i == 0) {
                arrItem[i] = this.firstHeader;

            } else if (i < this.listFirsItem.size() + 1) {
                arrItem[i] = this.listFirsItem.get(i - 1);

            } else if (i == this.listFirsItem.size() + 1) {
                arrItem[i] = this.secondHeader;

            } else {
                if (this.listSecondItem == null || this.listSecondItem.isEmpty()) {
                    break;
                } else {
                    arrItem[i] = this.listSecondItem.get(i - listItem.size() - 2);
                }
            }
        }

        this.expandSendItem = true;
        this.firstHeader.isExpand = true;
        this.firstHeader.title = this.getTitle(true, listItem);
        this.listItem = new ArrayList<>(Arrays.asList(arrItem));
        this.notifyDataSetChanged();
    }

    public void setSecondItems(List<TransferModel> listItem) {
        if (this.listSecondItem != null && !this.listSecondItem.isEmpty()) {
            this.listItem.removeAll(this.listSecondItem);
        }

        if (listItem != null && !listItem.isEmpty()) {
            this.listItem.addAll(listItem);
        }

        this.listSecondItem = listItem;
        this.expandReciveItem = true;
        this.secondHeader.isExpand = true;
        this.secondHeader.title = this.getTitle(false, listItem);
        this.notifyDataSetChanged();
    }

    @NotNull
    private String getTitle(boolean onTop, List<TransferModel> models) {
        if (models == null || models.isEmpty()) {
            if (this.onManager) {
                return onTop ? mContext.getString(R.string.cta_add_send_file) : mContext.getString(R.string.empty_receive_file);
            }
            return onTop ? mContext.getString(R.string.empty_download_file) : mContext.getString(R.string.empty_uploaded_file);
        }

        long size = 0;
        for (TransferModel model : models) {
            size += model.downloadSize;
        }

        if (this.onManager) {
            return onTop ? mContext.getString(R.string.send_file_info, models.size(), AppUtil.convertBytes(size))
                    : mContext.getString(R.string.received_file_info, models.size(), AppUtil.convertBytes(size));
        }
        return onTop ? mContext.getString(R.string.received_file_info, models.size(), AppUtil.convertBytes(size))
                : mContext.getString(R.string.upload_file_info, models.size(), AppUtil.convertBytes(size));
    }

    @Override
    public int getItemViewType(int position) {
        Object item = this.listItem.get(position);
        if (item == null) {
            return ITEM_HIDDEN;
        }

        if (item instanceof TransferModel) {
            if (position <= listFirsItem.size()) {
                return this.expandSendItem ? ITEM_NORMAL : ITEM_HIDDEN;
            } else {
                return this.expandReciveItem ? ITEM_NORMAL : ITEM_HIDDEN;
            }
        }

        if (item instanceof HeaderItem) {
            return ITEM_HEADER;
        }

        return ITEM_HIDDEN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.mContext);

        if (viewType == ITEM_NORMAL) {
            return new TransferItemHolder(ItemTransferObjectBinding.inflate(inflater, parent, false));
        }

        if (viewType == ITEM_HEADER) {
            return new HeaderItemHolder(ItemTransferHeaderBinding.inflate(inflater, parent, false));
        }

        return new HiddenItem(ItemTransferHeaderBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof HeaderItemHolder) {
            ((HeaderItemHolder) holder).onBindView(position);
        }

        if (holder instanceof TransferItemHolder) {
            ((TransferItemHolder) holder).onBindView(position);
        }
    }

    @Override
    public int getItemCount() {
        return this.listItem == null ? 0 : this.listItem.size();
    }

    private void showOrHideItem(int position) {
        if (position == 0) {
            if (this.listFirsItem == null || this.listFirsItem.isEmpty()) {
                Toast.makeText(mContext, this.onManager ? R.string.empty_send_file : R.string.empty_download_file, Toast.LENGTH_SHORT).show();
                return;
            }
            this.expandSendItem = !this.expandSendItem;
            this.firstHeader.isExpand = !this.firstHeader.isExpand;
        } else {
            if (this.listSecondItem == null || this.listSecondItem.isEmpty()) {
                Toast.makeText(mContext, this.onManager ? R.string.empty_receive_file : R.string.empty_uploaded_file, Toast.LENGTH_SHORT).show();
                return;
            }
            this.expandReciveItem = !this.expandReciveItem;
            this.secondHeader.isExpand = !this.secondHeader.isExpand;
        }

        this.notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return;
        }

        this.listItem.remove(position);

        if (position <= this.listFirsItem.size()) {
            this.listFirsItem.remove(position - 1);
            this.firstHeader.title = this.getTitle(true, this.listFirsItem);
        } else {
            int sendSize = this.listFirsItem.size();
            this.listSecondItem.remove(position - sendSize - 2);
            this.secondHeader.title = this.getTitle(false, this.listSecondItem);
        }

        this.notifyDataSetChanged();
    }

    public interface OnClickItemListener {

        void onClickSelectItem(int position);

        void onClickItem(int position);
    }

    static final class HiddenItem extends RecyclerView.ViewHolder {

        private final ItemTransferHeaderBinding mBinding;

        public HiddenItem(@NonNull ItemTransferHeaderBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            this.hideItem();
        }

        public void hideItem() {
            ViewGroup.LayoutParams params = this.mBinding.getRoot().getLayoutParams();
            params.height = 0;
            params.width = 0;
            this.mBinding.getRoot().setLayoutParams(params);
        }
    }

    static final class HeaderItem {

        public boolean isExpand;

        public String title;
    }

    final class HeaderItemHolder extends RecyclerView.ViewHolder {

        private final ItemTransferHeaderBinding mBinding;

        public HeaderItemHolder(@NonNull ItemTransferHeaderBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
        }

        public void onBindView(int position) {
            HeaderItem item = (HeaderItem) listItem.get(position);
            if (item == null) {
                return;
            }

            this.mBinding.tvTransferInfo.setText(item.title);
            this.mBinding.ivMore.setSelected(item.isExpand);

            this.mBinding.getRoot().setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    showOrHideItem(position);
                }
            });
        }
    }

    final class TransferItemHolder extends RecyclerView.ViewHolder {

        private final ItemTransferObjectBinding mBinding;

        public TransferItemHolder(@NonNull ItemTransferObjectBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
        }

        public void onBindView(int position) {
            TransferModel model = (TransferModel) listItem.get(position);
            if (model == null) {
                return;
            }

            if (model.type == ConnectUtils.TRASFER_OBJECT_SEND
                    || model.type == ConnectUtils.TRASFER_OBJECT_RECEIVE
                    || model.type == ConnectUtils.TRASFER_OBJECT_UPLOAD) {
                ListItem file = new ListItem(model.realPaths);
                if (!file.exists()) {
                    return;
                }

                file.setType(AppUtil.getMimeTypeByName(file.getName()));
                int resId = AppUtil.getMineTypeIcon(file);

                if (file.getType() == BrowserType.IMAGE || file.getType() == BrowserType.VIDEO) {
                    Glide.with(mContext)
                            .load(file.path)
                            .override(65)
                            .error(file.getType() == BrowserType.IMAGE ? R.drawable.ic_file_image : R.drawable.ic_file_video)
                            .into(this.mBinding.ivThumb);

                    this.mBinding.ivThumb.setBackgroundResource(R.drawable.bg_image_border);

                } else if (file.getType() == BrowserType.APK) {
                    Drawable apkIcon = AppUtil.getApkIcon(mContext, file.getPath());
                    if (apkIcon != null) {
                        this.mBinding.ivThumb.setImageDrawable(apkIcon);
                    } else {
                        this.mBinding.ivThumb.setImageResource(R.drawable.ic_file_apk);
                    }

                } else {
                    this.mBinding.ivThumb.setImageResource(resId);
                    this.mBinding.ivThumb.setBackgroundColor(ContextCompat.getColor(mContext, R.color.transparent));
                }

            } else {
                int resId = AppUtil.getMineTypeIcon(model.fileName);
                this.mBinding.ivThumb.setImageResource(resId);
            }

            this.mBinding.tvTitle.setText(model.fileName);
            this.mBinding.tvFileInfo.setText(AppUtil.convertBytes(model.downloadSize));
            this.mBinding.pbDownload.setVisibility(model.progress > 0 && model.progress < 100 ? View.VISIBLE : View.GONE);
            this.mBinding.pbUpload.setVisibility(View.GONE);

            if (model.type == ConnectUtils.TRASFER_OBJECT_SEND) {
                this.mBinding.ivSelectStage.setImageResource(R.drawable.ic_transfer_close_select);
            }

            if (model.type == ConnectUtils.TRASFER_OBJECT_RECEIVE) {
                this.mBinding.ivSelectStage.setImageResource(R.drawable.ic_library_delete);
            }

            if (model.type == ConnectUtils.TRASFER_OBJECT_DOWNLOAD) {
                this.mBinding.ivSelectStage.setImageResource(model.progress == 0 ? R.drawable.ic_transfer_download : R.drawable.ic_transfer_close_select);
                this.mBinding.pbDownload.setProgress(model.progress);
            }

            if (model.type == ConnectUtils.TRASFER_OBJECT_UPLOAD) {
                this.mBinding.ivSelectStage.setImageResource(R.drawable.ic_transfer_close_select);
                this.mBinding.pbDownload.setVisibility(View.GONE);
                this.mBinding.pbUpload.setVisibility(model.progress != 0 ? View.VISIBLE : View.GONE);
                this.mBinding.pbUpload.setProgress(model.progress);

                if (model.progress == 0) {
                    this.mBinding.tvFileInfo.setText(AppUtil.convertBytes(model.downloadSize));
                } else if (model.progress == 100) {
                    this.mBinding.tvFileInfo.setText(R.string.wait_upload_msg);
                } else {
                    String uploaded = AppUtil.convertBytes((model.progress * model.downloadSize) / 100);
                    this.mBinding.tvFileInfo.setText(mContext.getString(R.string.processing, uploaded, AppUtil.convertBytes(model.downloadSize), model.progress));
                }
            }

            this.mBinding.ivSelectStage.setOnClickListener(new OnceClick() {
                @Override
                public void onSingleClick(View v) {
                    if (listener != null) {
                        listener.onClickSelectItem(position);
                    }
                }
            });

        }
    }
}
