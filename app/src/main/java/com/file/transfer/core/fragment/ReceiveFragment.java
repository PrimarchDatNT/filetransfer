package com.file.transfer.core.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.file.transfer.R;
import com.file.transfer.core.ConnectUtils;
import com.file.transfer.core.TransferManagerAdapter;
import com.file.transfer.core.event.UpdateEventTag;
import com.file.transfer.core.event.UpdateFileEvent;
import com.file.transfer.core.model.TransferModel;
import com.file.transfer.core.util.AppUtil;
import com.file.transfer.core.util.OnceClick;
import com.file.transfer.databinding.DialogRenameLibararyBinding;
import com.file.transfer.databinding.FragmentReceiveBinding;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rx2androidnetworking.Rx2AndroidNetworking;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ReceiveFragment extends Fragment implements TransferManagerAdapter.OnClickItemListener {

    private static final int NO_CONNECTION = 0;
    private static final int WRONG_ADRESS = 1;
    private static final int REQUEST_SUCCESS = 2;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_DOWNLOAD_CONTENT_HREF = "/content_download";
    private static final String ARG_DOWNLOAD_FILE_HREF = "/download/";
    private static final String ARG_PING_SERVER = "/ping";

    private String serverAddress;
    private Context context;
    private Callback callback;
    private List<TransferModel> listUpload;
    private List<TransferModel> listDownloadItem;
    private TransferManagerAdapter mAdapter;
    private CompositeDisposable disposable;
    private Map<TransferModel, Disposable> managerTask;
    private FragmentReceiveBinding binding;
    private CountDownTimer countDownTimer;

    public ReceiveFragment() {
    }

    @NotNull
    public static ReceiveFragment newInstance(String param1) {
        ReceiveFragment fragment = new ReceiveFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
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
            this.serverAddress = arguments.getString(ARG_PARAM1);
        }

        this.managerTask = new HashMap<>();
        this.listUpload = new ArrayList<>();
        this.listDownloadItem = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentReceiveBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.binding.fabUpload.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onSelectUploadFile();
                }
            }
        });

        this.binding.tvEnableWifi.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onEnableWifi();
                }
            }
        });

        this.binding.ivScanQr.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                if (callback != null) {
                    callback.onScanAddress();
                }
            }
        });

        this.binding.tvInputAddress.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                showInputAdressDialog();
            }
        });

        this.mAdapter = new TransferManagerAdapter(this.context, false);
        this.mAdapter.setListener(this);
        this.binding.rvReceive.setLayoutManager(new LinearLayoutManager(this.context));
        this.binding.rvReceive.setAdapter(this.mAdapter);
        this.binding.rvReceive.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == 0 && listDownloadItem != null && listDownloadItem.isEmpty()
                        || listDownloadItem != null && !listDownloadItem.isEmpty() && position == listDownloadItem.size()) {
                    outRect.bottom = (int) getResources().getDimension(R.dimen.px4);
                }

                if (parent.getAdapter() != null && position == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = (int) getResources().getDimension(R.dimen.px80);
                }
            }
        });

        this.countDownTimer = new CountDownTimer(Long.MAX_VALUE, 5000) {
            @Override
            public void onTick(long millisUntilFinished) {
                loadData();
            }

            @Override
            public void onFinish() {

            }
        };
        this.countDownTimer.start();
        this.loadData();
        this.mAdapter.setSecondItems(this.listUpload);
    }

    public void loadData() {
        if (!ConnectUtils.isValidUrl(this.serverAddress)) {
            Toast.makeText(this.context, !AppUtil.isNetworkConnected(this.context) ? R.string.no_connection_err
                    : R.string.connect_ip_address_err, Toast.LENGTH_SHORT).show();
            this.showErrorLayout(!AppUtil.isNetworkConnected(this.context) ? NO_CONNECTION : WRONG_ADRESS);
            return;
        }

//        this.binding.animLoading.setVisibility(View.VISIBLE);

        Rx2AndroidNetworking.get(this.serverAddress + ARG_DOWNLOAD_CONTENT_HREF)
                .doNotCacheResponse()
                .build()
                .getStringSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        subcribeTask(null, d);
                    }

                    @Override
                    public void onSuccess(@NonNull String data) {
                        onRequestSucces(data);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.connect_ip_address_err, Toast.LENGTH_SHORT).show();
                        showErrorLayout(WRONG_ADRESS);
                    }
                });
    }

    public void uploadFileToServer(String path) {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this.context, R.string.file_upload_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this.context, R.string.file_upload_not_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        TransferModel model = new TransferModel();
        model.fileName = file.getName();
        model.downloadSize = file.length();
        model.realPaths = path;
        model.type = ConnectUtils.TRASFER_OBJECT_UPLOAD;
        this.listUpload.add(model);
        this.mAdapter.setSecondItems(this.listUpload);
        int position = this.mAdapter.getItemCount() - 1;
        this.uploadFile(position, file);
    }

    private void uploadFile(int position, File file) {
        TransferModel model = (TransferModel) this.mAdapter.getItemPosition(position);
        if (model == null) {
            return;
        }

        this.binding.fabUpload.setVisibility(View.GONE);

        Single.create((SingleOnSubscribe<Integer>) emitter ->
                        emitter.onSuccess(this.uploadFile(file,
                                this.serverAddress + "/upload",
                                (uploadedBytes, totalBytes) -> this.requireActivity().runOnUiThread(() -> {

                                    if (this.managerTask.get(model) == null) {
                                        model.progress = 0;
                                        this.binding.fabUpload.setVisibility(View.VISIBLE);
                                    } else {
                                        model.progress = (int) (100 * uploadedBytes / totalBytes);
                                    }

                                    int positionByPath = this.mAdapter.getPositionByPath(model.realPaths);
                                    if (positionByPath < 0) {
                                        return;
                                    }

                                    this.mAdapter.notifyItemChanged(positionByPath);
                                })))).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        subcribeTask(model, d);
                    }

                    @Override
                    public void onSuccess(@NonNull Integer responseCode) {
                        Toast.makeText(context, responseCode == 200 ? R.string.upload_success_msg : R.string.upload_err_msg, Toast.LENGTH_SHORT).show();
                        onTaskComplete(model);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(context, R.string.upload_err_msg, Toast.LENGTH_SHORT).show();
                        onTaskComplete(model);
                    }
                });
    }

    public int uploadFile(@NotNull File sourceFile, String upLoadServerUri, UploadCallback uploadCallback) {
        int resCode = 0;
        HttpURLConnection conn;
        DataOutputStream dos;

        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            URL url = new URL(upLoadServerUri);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("content-length", String.valueOf(sourceFile.length()));
            conn.setRequestProperty("file_name", sourceFile.getName());
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestMethod("POST");
            conn.setFixedLengthStreamingMode(sourceFile.length());

            long size = sourceFile.length();
            long uploaded = 0;
            int bytesAvailable = fis.available();
            int bufferSize = Math.min(bytesAvailable, 4096);
            byte[] bytes = new byte[bufferSize];
            dos = new DataOutputStream(conn.getOutputStream());
            int read = fis.read(bytes, 0, bufferSize);
            while (read > 0) {
                dos.write(bytes, 0, read);
                bufferSize = Math.min(bytesAvailable, 4096);
                read = fis.read(bytes, 0, bufferSize);
                uploaded += read;
                uploadCallback.uploadStage(uploaded, size);
            }

            resCode = conn.getResponseCode();

            fis.close();
            dos.flush();
            dos.close();
            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return resCode;
    }

    private void showInputAdressDialog() {
        DialogRenameLibararyBinding binding = DialogRenameLibararyBinding.inflate(this.getLayoutInflater());
        Dialog dialog = AppUtil.createDialog(this.requireContext());
        dialog.setContentView(binding.getRoot());

        binding.tvDialogCancelCompressTitle.setText(R.string.input_address_title);
        binding.tvConfirmRename.setText(R.string.cta_connect);
        binding.edFileRename.setText(ConnectUtils.HTTP_HEADER);
        binding.edFileRename.setSelection(ConnectUtils.HTTP_HEADER.length());

        binding.tvCancelRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                AppUtil.hideKeyboard(context, binding.edFileRename);
                dialog.dismiss();
            }
        });

        binding.tvConfirmRename.setOnClickListener(new OnceClick() {
            @Override
            public void onSingleClick(View v) {
                Editable text = binding.edFileRename.getText();
                if (text == null || TextUtils.isEmpty(text.toString().trim())) {
                    Toast.makeText(context, R.string.empty_input_address_msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                AppUtil.hideKeyboard(context, binding.edFileRename);
                dialog.dismiss();
                serverAddress = text.toString();
                loadData();
            }
        });

        AppUtil.showKeyboard(this.context);
        AppUtil.showFullScreenDialog(dialog);
    }

    private void showErrorLayout(int status) {
        this.binding.animLoading.setVisibility(View.GONE);
        this.binding.flReceiveContent.setVisibility(status != REQUEST_SUCCESS ? View.GONE : View.VISIBLE);
        this.binding.clError.setVisibility(status != REQUEST_SUCCESS ? View.VISIBLE : View.GONE);
        this.binding.tvEnableWifi.setVisibility(status == NO_CONNECTION ? View.VISIBLE : View.GONE);
        this.binding.rlScanInput.setVisibility(status == WRONG_ADRESS ? View.VISIBLE : View.GONE);

        if (status == NO_CONNECTION) {
            this.binding.tvErrMessage.setText(R.string.err_connect_layout_msg);
        }

        if (status == WRONG_ADRESS) {
            this.binding.tvErrMessage.setText(R.string.err_adderss_layout_msg);
        }
    }

    private void onRequestSucces(String data) {
        if (data != null && !TextUtils.isEmpty(data)) {
            try {
                this.listDownloadItem = new Gson().fromJson(data, new TypeToken<List<TransferModel>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }

            if (this.listDownloadItem == null) {
                this.listDownloadItem = new ArrayList<>();
                this.showErrorLayout(WRONG_ADRESS);
            } else {
                for (TransferModel model : this.listDownloadItem) {
                    model.type = ConnectUtils.TRASFER_OBJECT_DOWNLOAD;
                }
                this.showErrorLayout(REQUEST_SUCCESS);
            }
        }

        this.mAdapter.setFirstItems(this.listDownloadItem);
    }

    @Override
    public void onClickSelectItem(int position) {
/*        if (!AppUtil.isNetworkConnected(this.context)) {
            Toast.makeText(this.context, R.string.download_err_msg, Toast.LENGTH_SHORT).show();
            return;
        }*/

        TransferModel model = (TransferModel) this.mAdapter.getItemPosition(position);
        if (model == null) {
            return;
        }

        if (model.type == ConnectUtils.TRASFER_OBJECT_UPLOAD) {
            if (model.progress > 0) {
                this.showCancelDialog(false, model);
                return;
            }

            this.showRemoveDialog(position);

            return;
        }

        if (model.type == ConnectUtils.TRASFER_OBJECT_DOWNLOAD) {
            if (model.progress > 0) {
                this.showCancelDialog(true, model);
                return;
            }

            Rx2AndroidNetworking.get(this.serverAddress + ARG_PING_SERVER + "/" + model.downloadId)
                    .doNotCacheResponse()
                    .build()
                    .getStringSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<String>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                            subcribeTask(null, d);
                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull String data) {
                            if (TextUtils.isEmpty(data)) {
                                Toast.makeText(context, R.string.connect_ip_address_err, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (TextUtils.equals(data, ConnectUtils.UNABLE_DOWNLOAD)) {
                                Toast.makeText(context, R.string.unable_download_file_msg, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (TextUtils.equals(data, ConnectUtils.AVAILABLE_DOWNLOAD)) {
                                downloadItem(model);
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(context, R.string.request_timeout, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void downloadItem(@NotNull TransferModel model) {
        String url = this.serverAddress + ARG_DOWNLOAD_FILE_HREF + model.downloadId;
        String uniqueFileName = ConnectUtils.getUniqueFileName(ConnectUtils.getDownloadDocumentFile(), model.fileName, false);

        Rx2AndroidNetworking.download(url, ConnectUtils.getDownloadDir(), uniqueFileName)
                .build()
                .setDownloadProgressListener((bytesDownloaded, totalBytes) -> {
                    int positionByPath = this.mAdapter.getPositionByPath(model.realPaths);
                    if (positionByPath < 0 || totalBytes <= 0) {
                        return;
                    }

                    if (this.managerTask.get(model) == null) {
                        model.progress = 0;
                    } else {
                        model.progress = (int) ((bytesDownloaded * 100) / totalBytes);
                    }

                    this.mAdapter.notifyItemChanged(positionByPath);
                })
                .getDownloadCompletable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        subcribeTask(model, d);
                    }

                    @Override
                    public void onComplete() {
                        onDownloadSucces(new File(ConnectUtils.getDownloadDir() + "/" + uniqueFileName));
                        onTaskComplete(model);
                        binding.fabUpload.setVisibility(View.VISIBLE);

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        onTaskComplete(model);
                        binding.fabUpload.setVisibility(View.VISIBLE);
                    }
                });
    }

    public void onTaskComplete(@NotNull TransferModel model) {
        model.progress = 0;
        this.mAdapter.notifyItemChanged(this.mAdapter.getPositionByPath(model.realPaths));
        this.managerTask.remove(model);
        this.binding.fabUpload.setVisibility(View.VISIBLE);
    }

    private void showRemoveDialog(int position) {
        new AlertDialog.Builder(this.requireContext())
                .setTitle(R.string.remove_transfer_item_title)
                .setMessage(R.string.remove_transfer_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    mAdapter.removeItem(position);
                }).create()
                .show();
    }

    private void showCancelDialog(boolean onDownload, TransferModel model) {
        if (this.disposable == null) {
            return;
        }

        Disposable task = this.managerTask.get(model);
        if (task == null) {
            return;
        }

        new AlertDialog.Builder(this.requireContext())
                .setTitle(onDownload ? R.string.cancel_download : R.string.cancel_upload)
                .setMessage(onDownload ? R.string.cancel_download_msg : R.string.cancel_upload_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    if (disposable.remove(task)) {
                        managerTask.remove(model);
                        if (onDownload) {
                            model.progress = 0;
                            mAdapter.notifyItemChanged(mAdapter.getPositionByPath(model.realPaths));
                            return;
                        }

                        mAdapter.removeItem(mAdapter.getPositionByPath(model.realPaths));
                        binding.fabUpload.setVisibility(View.VISIBLE);
                    }
                }).create()
                .show();
    }

    private void onDownloadSucces(@NotNull File file) {
        if (!file.exists() || TextUtils.isEmpty(this.serverAddress)) {
            return;
        }

        this.context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        EventBus.getDefault().post(new UpdateFileEvent(UpdateEventTag.TAG_UPDATE_LOCAL_DATA));
        Toast.makeText(this.context, this.context.getString(R.string.download_success_msg), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClickItem(int position) {
    }

    private void subcribeTask(TransferModel model, Disposable d) {
        if (model == null) {
            return;
        }
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }

        this.disposable.add(d);
        this.managerTask.put(model, d);
    }

    @Override
    public void onDetach() {
        if (this.managerTask != null) {
            for (Disposable disposable : this.managerTask.values()) {
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }

        System.gc();
        if (this.countDownTimer != null) {
            this.countDownTimer.cancel();
        }
        super.onDetach();
    }

    public interface UploadCallback {

        void uploadStage(long uploadedBytes, long totalBytes);
    }

    public interface Callback {

        void onSelectUploadFile();

        void onEnableWifi();

        void onScanAddress();
    }
}