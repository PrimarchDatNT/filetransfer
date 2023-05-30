package com.file.transfer.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.file.transfer.R;
import com.file.transfer.core.fragment.BottomSelectItemDialog;
import com.file.transfer.core.model.BaseFileItem;
import com.file.transfer.core.model.BrowserType;
import com.file.transfer.core.model.ListItem;
import com.file.transfer.core.model.SortType;
import com.file.transfer.core.util.AppKeyConstant;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.FileUtils;
import com.file.transfer.core.view.BottomConfigAdapter;
import com.file.transfer.core.view.CustomPopupMenu;
import com.file.transfer.databinding.ActivitySelectFileTransferBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class SelectFileTransferActivity extends AppCompatActivity implements BottomConfigAdapter.OnItemConfigListener,
        SelectFileAdapter.Callback, FolderAdapter.Callback, BottomSelectItemDialog.Callback {

    /*private static final int CUR_EXTERNAL_ROOT = 0;*/
    private static final int CUR_SDCARD_ROOT = 1;

    private boolean isScrolling;
    private boolean isChangeSelect;
    private int currentRoot;

    private String extCardPath;
    private String internalStoragePath;
    private List<String> folderPaths;
    private ArrayList<String> selectPaths;
    private List<ListItem> listItem;
    private FolderAdapter folderAdapter;
    private SelectFileAdapter mAdapter;
    private CustomPopupMenu popupMenuCard;
    private CompositeDisposable disposable;
    private ActivitySelectFileTransferBinding mBinding;

    private boolean isSelectOnlyFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    private void init() {
        this.initView();
        this.initData();
        this.initPopup();
    }

    private void initView() {
        this.initLayout();
        this.initRvDetail();
        this.initRvFolder();
    }

    private void initLayout() {
        this.mBinding = ActivitySelectFileTransferBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.mBinding.getRoot());
    }

    private void initRvDetail() {
        if (getIntent() != null) {
            this.isSelectOnlyFile = getIntent().getBooleanExtra(ConnectUtils.EXTRA_SELECT_ONLY_FILE, false);
        }
        this.mBinding.flSelectItem.setVisibility(this.isSelectOnlyFile ? View.GONE : View.VISIBLE);
        this.mAdapter = new SelectFileAdapter(this, this.isSelectOnlyFile);
        this.mAdapter.setCallback(this);
        this.mBinding.rvDetailFolder.setLayoutManager(new LinearLayoutManager(this));
        this.mBinding.rvDetailFolder.setAdapter(this.mAdapter);
        this.mBinding.rvDetailFolder.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@androidx.annotation.NonNull Rect outRect, @androidx.annotation.NonNull View view, @androidx.annotation.NonNull RecyclerView parent, @androidx.annotation.NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (parent.getAdapter() != null && position == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = (int) getResources().getDimension(R.dimen.px100);
                }
            }
        });
    }

    private void initRvFolder() {
        this.folderAdapter = new FolderAdapter(this, this);
        this.mBinding.rvParentPath.setAdapter(this.folderAdapter);
        this.mBinding.rvParentPath.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        this.mBinding.rvParentPath.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScrolling = false;
                }

                if (newState == RecyclerView.SCROLL_STATE_SETTLING || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isScrolling = true;
                }
            }
        });
    }

    private void initData() {
        this.listItem = new ArrayList<>();
        this.extCardPath = FileUtils.getExtCardPath(this, false);
        this.internalStoragePath = Environment.getExternalStorageDirectory().getPath();
        this.folderPaths = new ArrayList<>();
        this.folderPaths.add(this.internalStoragePath);
        this.folderAdapter.setRootPaths(this.folderPaths);
        this.readFiles(this.internalStoragePath);

        Intent intent = this.getIntent();
        if (intent == null || this.isSelectOnlyFile) {
            return;
        }

        this.selectPaths = intent.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH);

        this.updateTotalSelect();
    }

    private void initPopup() {
        List<String> listMore = new ArrayList<>();
        listMore.add(this.getString(R.string.intenal_storage_title));
        listMore.add(this.getString(R.string.sd_card_title));
        this.popupMenuCard = new CustomPopupMenu(this, this.mBinding.ivHome);
        this.popupMenuCard.setWidthWrapContent(true);
        this.popupMenuCard.setDatas(listMore, false);
        this.popupMenuCard.setItemConfigListener(this);
    }

    public void onClickGoPremium(View view) {
//        BillingActivityDefault.openBillingDefault(this, "");
    }

    public void onClickBack(View view) {
        if (this.isChangeSelect) {
            this.showCofirmDialog();
            return;
        }
        super.onBackPressed();
    }

    public void onCickRootDir(View view) {
        this.showPopupChangeCard();
    }

    public void onClickMore(View view) {
        if (this.selectPaths == null || this.selectPaths.isEmpty()) {
            return;
        }
        BottomSelectItemDialog dialog = new BottomSelectItemDialog(this);
        dialog.showEx(this.getSupportFragmentManager(), this.selectPaths);
    }

    public void onClickSend(View view) {
        this.onConfrimSend();
    }

    private void showCofirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.save_change_title)
                .setMessage(R.string.save_change_msg)
                .setNegativeButton(R.string.cta_exit_compress, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .create()
                .show();
    }

    private void updateTotalSelect() {
        if (this.selectPaths == null || this.selectPaths.isEmpty()) {
            this.mBinding.tvSendInfo.setText(R.string.empty_select_file_send);
            return;
        }

        long size = 0;
        for (String path : this.selectPaths) {
            File file = new File(path);
            if (file.exists()) {
                size += file.length();
            }
        }

        this.mBinding.tvSendInfo.setText(this.getString(R.string.send_file_info, this.selectPaths.size(), AppUtil.convertBytes(size)));
    }

    private void showPopupChangeCard() {
        if (this.popupMenuCard == null || this.popupMenuCard.isShowwing()) {
            return;
        }
        this.popupMenuCard.show();
    }

    private void readFiles(String pathRoot) {
        Single.create((SingleOnSubscribe<List<ListItem>>) emitter -> {

                    File folderDir = new File(pathRoot);
                    if (!folderDir.exists() || folderDir.length() == 0) {
                        emitter.onError(new NullPointerException());
                        return;
                    }

                    File[] childs = folderDir.listFiles();
                    if (childs == null || childs.length == 0) {
                        emitter.onError(new NullPointerException());
                        return;
                    }

                    List<ListItem> listItem = new ArrayList<>();
                    for (File file : childs) {
                        if (file.exists()) {
                            ListItem item = new ListItem(file.getPath());
                            if (item.isDir()) {
                                item.setType(BrowserType.DIRECTORY);
                            } else {
                                item.setType(AppUtil.getMimeTypeByName(item.getName()));
                            }
                            listItem.add(item);
                        }
                    }

                    if (listItem.isEmpty()) {
                        emitter.onSuccess(listItem);
                        return;
                    }

                    if (this.selectPaths == null || this.selectPaths.isEmpty()) {
                        for (ListItem item : listItem) {
                            item.setSelected(false);
                        }

                        emitter.onSuccess(listItem);
                        return;
                    }

                    List<Integer> selectedPos = new ArrayList<>();

                    for (int i = 0; i < listItem.size(); i++) {
                        for (int j = 0; j < this.selectPaths.size(); j++) {
                            if (TextUtils.equals(listItem.get(i).getPath(), this.selectPaths.get(j))) {
                                selectedPos.add(i);
                                break;
                            }
                        }

                        if (selectedPos.size() == this.selectPaths.size()) {
                            break;
                        }
                    }

                    if (!selectedPos.isEmpty()) {
                        for (Integer position : selectedPos) {
                            listItem.get(position).setSelected(true);
                        }
                    }

                    emitter.onSuccess(listItem);

                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<ListItem>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        subcribeTask(d);
                    }

                    @Override
                    public void onSuccess(@NonNull List<ListItem> items) {
                        showItems(items);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        showItems(new ArrayList<>());
                    }
                });
    }

    private void showItems(List<ListItem> items) {
        this.listItem = new ArrayList<>(items);
        Collections.sort(this.listItem, new BaseFileItem.SortFiles(SortType.SORT_NAME));
        this.mAdapter.setListItem(this.listItem);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClick(String name, int position) {
        if (this.popupMenuCard == null) {
            return;
        }

        if (position == this.currentRoot) {
            this.popupMenuCard.dismiss();
            return;
        }

        if (position == CUR_SDCARD_ROOT && TextUtils.isEmpty(this.extCardPath)) {
            Toast.makeText(this, R.string.sd_card_desc, Toast.LENGTH_SHORT).show();
            this.popupMenuCard.dismiss();
            return;
        }

        this.currentRoot = position;
        boolean isSdCard = this.currentRoot == CUR_SDCARD_ROOT;

        String curDir = isSdCard ? this.extCardPath : this.internalStoragePath;
        this.mBinding.ivHome.setImageResource(isSdCard ? R.drawable.ic_menu_sd_card : R.drawable.ic_menu_storage);
        this.folderPaths.clear();
        this.folderPaths.add(curDir);
        this.folderAdapter.notifyDataSetChanged();
        this.readFiles(curDir);
        this.popupMenuCard.dismiss();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClickFolderItem(int position) {
        if (this.folderPaths == null || this.folderPaths.isEmpty()) {
            return;
        }

        int currentSize = this.folderPaths.size();
        if (position == currentSize - 1) {
            return;
        }

        List<String> removePaths = new ArrayList<>();
        for (int i = position + 1; i < currentSize; i++) {
            removePaths.add(this.folderPaths.get(i));
        }

        this.folderPaths.removeAll(removePaths);
        this.folderAdapter.notifyDataSetChanged();
        this.readFiles(this.folderPaths.get(this.folderPaths.size() - 1));
    }

    @Override
    public void onClickItem(int position) {
        ListItem item = this.mAdapter.getItemPosition(position);
        if (item == null) {
            return;
        }

        String path = item.path;
        this.folderPaths.add(path);
        this.folderAdapter.notifyDataSetChanged();
        this.scrollBottom();
        this.readFiles(path);
    }

    @Override
    public void onClickCheckItem(int position) {
        ListItem item = this.mAdapter.getItemPosition(position);
        if (item == null) {
            return;
        }

        if (this.isSelectOnlyFile) {
            Intent data = new Intent();
            data.putExtra(AppKeyConstant.EXTRA_BROWSE_RESULT, item.path);
            this.setResult(Activity.RESULT_OK, data);
            this.finish();
            return;
        }

        if (this.selectPaths == null) {
            this.selectPaths = new ArrayList<>();
        }

        boolean selected = !item.isSelected();
        List<String> pathsAfterAdd = new ArrayList<>();
        pathsAfterAdd.add(item.getPath());
        pathsAfterAdd.addAll(this.selectPaths);


        if (selected) {
            this.selectPaths.add(item.path);
        } else {
            this.selectPaths.remove(item.path);
        }

        item.setSelected(selected);
        this.mAdapter.notifyItemChanged(position);

        this.updateTotalSelect();

        if (!this.isChangeSelect) {
            this.isChangeSelect = true;
        }
    }

    private void scrollBottom() {
        try {
            if (this.isScrolling) {
                return;
            }

            int scrollToPosition = this.folderPaths == null || this.folderPaths.isEmpty() ? 0 : this.folderPaths.size() - 1;
            if (scrollToPosition <= 0) {
                return;
            }

            this.mBinding.rvParentPath.smoothScrollToPosition(scrollToPosition);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfrimSend() {
        Intent data = new Intent();
        data.putStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH, this.selectPaths);
        this.setResult(Activity.RESULT_OK, data);
        this.finish();
    }

    @Override
    public void onRemoveItem(int position) {
        if (this.selectPaths == null || this.selectPaths.isEmpty()) {
            return;
        }

        String path = this.selectPaths.get(position);

        if (TextUtils.isEmpty(path)) {
            this.selectPaths.remove(position);
            return;
        }

        int size = this.listItem.size();
        for (int i = 0; i < size; i++) {
            ListItem item = this.listItem.get(i);
            if (item != null && TextUtils.equals(item.path, path)) {
                item.setSelected(false);
                this.mAdapter.notifyItemChanged(i);
                break;
            }
        }

        this.selectPaths.remove(position);

        if (!this.isChangeSelect) {
            this.isChangeSelect = true;
        }
    }

    @Override
    public void onCloseMore() {
        this.updateTotalSelect();
    }

    @Override
    public void onBackPressed() {
        if (this.folderPaths == null || this.folderPaths.isEmpty() || this.folderPaths.size() == 1) {
            if (this.isChangeSelect) {
                this.showCofirmDialog();
            } else {
                super.onBackPressed();
            }
            return;
        }

        this.onClickFolderItem(this.folderPaths.size() - 2);
    }

    private void subcribeTask(Disposable disposable) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        if (this.disposable != null) {
            this.disposable.dispose();
        }

        System.gc();
        super.onDestroy();
    }

    @Override
    public void onDelete(String name, int position) {
    }

}