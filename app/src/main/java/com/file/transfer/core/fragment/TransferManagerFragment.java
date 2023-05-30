package com.file.transfer.core.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.file.transfer.R;
import com.file.transfer.core.ConnectUtils;
import com.file.transfer.core.TransferManagerAdapter;
import com.file.transfer.core.model.TransferModel;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.FragmentTransferManagerBinding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransferManagerFragment extends Fragment implements TransferManagerAdapter.OnClickItemListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Context context;
    private ArrayList<String> sharePaths;
    private ArrayList<String> listReceive;
    private List<TransferModel> listSendItem;
    private List<TransferModel> listReceiveItem;
    private TransferManagerAdapter mAdapter;
    private FragmentTransferManagerBinding mBinding;

    private Callback callback;

    public TransferManagerFragment() {
    }

    @NotNull
    public static TransferManagerFragment newInstance(ArrayList<String> sharePaths, ArrayList<String> listReceive) {
        TransferManagerFragment fragment = new TransferManagerFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PARAM1, sharePaths);
        args.putStringArrayList(ARG_PARAM2, listReceive);
        fragment.setArguments(args);
        return fragment;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setListShare(ArrayList<String> uploadedPaths) {
        this.sharePaths = new ArrayList<>(uploadedPaths);
        this.listSendItem = this.getListItem(ConnectUtils.TRASFER_OBJECT_SEND, uploadedPaths);

        if (this.mAdapter != null) {
            this.mAdapter.setFirstItems(this.listSendItem);
        }
    }

    public void setListReceive(ArrayList<String> uploadedPaths) {
        this.listReceive = new ArrayList<>(uploadedPaths);
        this.listReceiveItem = this.getListItem(ConnectUtils.TRASFER_OBJECT_RECEIVE, uploadedPaths);

        if (this.mAdapter != null) {
            this.mAdapter.setSecondItems(this.listReceiveItem);
        }
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
        if (arguments == null) {
            return;
        }

        this.sharePaths = arguments.getStringArrayList(ARG_PARAM1);
        this.listReceive = arguments.getStringArrayList(ARG_PARAM2);
        this.listSendItem = this.getListItem(ConnectUtils.TRASFER_OBJECT_SEND, this.sharePaths);
        this.listReceiveItem = this.getListItem(ConnectUtils.TRASFER_OBJECT_RECEIVE, this.listReceive);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mBinding = FragmentTransferManagerBinding.inflate(inflater, container, false);
        return this.mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mAdapter = new TransferManagerAdapter(this.context, true);
        this.mAdapter.setListener(this);
        this.mAdapter.setFirstItems(this.listSendItem);
        this.mAdapter.setSecondItems(this.listReceiveItem);
        this.mBinding.rvTransferManager.setAdapter(this.mAdapter);
        this.mBinding.rvTransferManager.setLayoutManager(new LinearLayoutManager(this.context));
        this.mBinding.rvTransferManager.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == 0 && listSendItem != null && listSendItem.isEmpty()
                        || listSendItem != null && !listSendItem.isEmpty() && position == listSendItem.size()) {
                    outRect.bottom = (int) getResources().getDimension(R.dimen.px4);
                }

                if (parent.getAdapter() != null && position == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = (int) getResources().getDimension(R.dimen.px100);
                }
            }
        });

        this.mBinding.fabUpload.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onSelectFileShare();
                }
            }
        });
    }

    @NotNull
    private List<TransferModel> getListItem(int type, ArrayList<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }

        List<TransferModel> listItem = new ArrayList<>();

        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));

            if (file.exists()) {
                TransferModel model = new TransferModel();
                model.fileName = file.getName();
                model.downloadSize = file.length();
                model.realPaths = file.getAbsolutePath();
                model.type = type;
                listItem.add(model);
            }
        }

        return listItem;
    }

    @Override
    public void onClickSelectItem(int position) {
        this.showConfrimDialog(position);
    }

    private void removeSendFile(boolean isSendFile, int position) {
        if (this.mAdapter != null) {
            this.mAdapter.removeItem(position);
        }

        int sendSize = this.listSendItem == null ? 0 : this.listSendItem.size();
        int removePos = position - sendSize - 2;

        if (this.callback != null) {
            this.callback.onRemoveItem(isSendFile, isSendFile ? position - 1 : removePos);
        }
    }

    private void showConfrimDialog(int position) {
        boolean isSendFile = position <= this.listSendItem.size();
        new AlertDialog.Builder(this.requireContext())
                .setTitle(isSendFile ? R.string.rm_send_file_title : R.string.del_receive_file_title)
                .setMessage(isSendFile ? R.string.rm_send_file_msg : R.string.del_receive_file_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    removeSendFile(isSendFile, position);
                })
                .create()
                .show();
    }

    @Override
    public void onClickItem(int position) {
    }

    public interface Callback {

        void onRemoveItem(boolean isSendItem, int position);

        void onSelectFileShare();
    }
}