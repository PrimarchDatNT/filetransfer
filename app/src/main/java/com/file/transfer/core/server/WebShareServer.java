package com.file.transfer.core.server;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.file.transfer.R;
import com.file.transfer.core.CommunicationService;
import com.file.transfer.core.ConnectUtils;
import com.file.transfer.core.model.TransferModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class WebShareServer extends NanoHTTPD {

    private static final String REQ_UPLOAD = "_req_upload";
    private static final String REQ_DOWNLOAD = "_req_download";

    private static final LinkedList<String> REQUEST_POOL = new LinkedList<>();

    private final WeakReference<Context> mContext;
    private final AssetManager mAssetManager;

    private ArrayList<String> sharePaths;
    private List<TransferModel> listItem = new ArrayList<>();

    public WebShareServer(int port, @NotNull Context mContext) {
        super(port);
        this.mContext = new WeakReference<>(mContext);
        this.mAssetManager = mContext.getAssets();
    }

    public void setSharePaths(ArrayList<String> sharePaths) {
        this.sharePaths = sharePaths;
        this.listItem = this.getTransferObjects();
    }

    private synchronized void pollRequest() {
        REQUEST_POOL.poll();
    }

    private synchronized void managerRequest(String reqProperty) {
        REQUEST_POOL.add(reqProperty);
    }

    @NonNull
    private String renderWebPage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            InputStream inputStream = this.openFileFormAssets("home.html");
            int len;

            while ((len = inputStream.read()) != -1) {
                stream.write(len);
                stream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toString();
    }

    @NotNull
    private byte[] readAssetsFile(String pageName) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        InputStream inputStream = this.openFileFormAssets(pageName);
        int len;

        while ((len = inputStream.read()) != -1) {
            stream.write(len);
            stream.flush();
        }

        return stream.toByteArray();
    }

    private InputStream openFileFormAssets(String fileName) throws IOException {
        return this.mAssetManager.open("webshare" + File.separator + fileName);
    }

    @Override
    public Response serve(@NotNull IHTTPSession session) {

        String uri = session.getUri();
        Log.d(WebShareServer.class.getSimpleName(), "serve: " + uri);

        if (TextUtils.equals(uri, "/")) {
            return newFixedLengthResponse(this.renderWebPage());
        }

        if (uri.startsWith("/content_download")) {
            return this.getListContentResponse();
        }

        if (uri.startsWith("/upload")) {
            if (REQUEST_POOL.size() >= 5) {
                return this.getMessageResponse(Response.Status.OK, this.mContext.get().getString(R.string.sever_limited_msg));
            } else {
                try {
                    return this.uploadResponse(session);
                } catch (IOException e) {
                    e.printStackTrace();
                    this.pollRequest();
                    return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UPLOAD_FAIL_MSG);
                }
            }
        }

        if (uri.startsWith("/ping")) {
            try {
                long index = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                return this.getDownloadAvailableItem(index);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return this.getMessageResponse(Response.Status.OK, ConnectUtils.UNABLE_DOWNLOAD);
            }
        }

        if (uri.startsWith("/download")) {
            if (REQUEST_POOL.size() >= 5) {
                return this.getMessageResponse(Response.Status.OK, this.mContext.get().getString(R.string.sever_limited_msg));
            } else {
                try {
                    return this.getDownloadResponse(Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UNABLE_DOWNLOAD);
                }
            }
        }

        return this.getAssetsResponse(uri);
    }

    @NotNull
    private Response getMessageResponse(Response.Status status, String s) {
        return newFixedLengthResponse(status, NanoHTTPD.MIME_HTML, s);
    }

    @NotNull
    private Response getListContentResponse() {
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json", this.getTransferJson());
        response.addHeader("Access-Control-Allow-Origin", "* ");
        return response;
    }

    @Nullable
    private List<TransferModel> getTransferObjects() {
        if (this.sharePaths == null || this.sharePaths.isEmpty()) {
            return null;
        }

        List<TransferModel> listItem = new ArrayList<>();

        for (int i = 0; i < this.sharePaths.size(); i++) {
            File file = new File(this.sharePaths.get(i));

            if (file.exists()) {
                TransferModel model = new TransferModel();
                model.fileName = file.getName();
                model.downloadSize = file.length();
                model.realPaths = file.getAbsolutePath();
                model.downloadId = System.nanoTime();
                listItem.add(model);
            }
        }

        return listItem;
    }

    private String getTransferJson() {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return "[]";
        }

        return new Gson().toJson(new ArrayList<>(this.listItem), new TypeToken<List<TransferModel>>() {
        }.getType());
    }

    @Override
    public void stop() {
        super.stop();
        REQUEST_POOL.clear();
    }

    private String getSendPath(long downloadId) {
        if (this.listItem == null || this.listItem.isEmpty()) {
            return "";
        }
        for (TransferModel model : this.listItem) {
            if (model.downloadId == downloadId) {
                return model.realPaths;
            }
        }

        return "";
    }

    @NotNull
    private Response getDownloadAvailableItem(long index) {
        String sendPath = this.getSendPath(index);
        File file = new File(sendPath);
        return file.exists() ? this.getMessageResponse(Response.Status.OK, ConnectUtils.AVAILABLE_DOWNLOAD) : this.getMessageResponse(Response.Status.OK, ConnectUtils.UNABLE_DOWNLOAD);
    }

    @NotNull
    private Response getDownloadResponse(long index) {
        String sendPath = this.getSendPath(index);
        File file = new File(sendPath);
        if (!file.exists()) {
            return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UNABLE_DOWNLOAD);
        }

        Response response;
        try {
            FileInputStream data = new ReqInputStream(file);
            response = newFixedLengthResponse(Response.Status.OK, "file/*", data, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UNABLE_DOWNLOAD);
    }

    @NotNull
    private Response getAssetsResponse(@NotNull String uri) {
        try {
            byte[] byteData = this.readAssetsFile(uri.substring(1));
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteData);
            return newFixedLengthResponse(Response.Status.OK, uri.contains(".svg") ? "image/svg+xml" : "file/*", byteArrayInputStream, byteData.length);
        } catch (IOException e) {
            e.printStackTrace();
            return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UNABLE_DOWNLOAD);
        }
    }

    @NotNull
    private Response uploadResponse(@NotNull IHTTPSession session) throws IOException {

        String reqProperty = session.getHeaders().get("http-client-ip") + REQ_UPLOAD;
        this.managerRequest(reqProperty);

        long size = Long.parseLong(session.getHeaders().get("content-length"));
        String orgName = session.getHeaders().get("file_name");
        if (TextUtils.isEmpty(orgName)) {
            this.pollRequest();
            return this.getMessageResponse(Response.Status.NO_CONTENT, ConnectUtils.UPLOAD_FAIL_MSG);
        }

        String uniqueFileName = ConnectUtils.getUniqueFileName(ConnectUtils.getUploadDocumentFile(), orgName, true);
        File file = new File(ConnectUtils.getUploadDir() + File.separator + uniqueFileName);

        OutputStream outputStream = new FileOutputStream(file);
        int rlen = 0;

        byte[] buf = new byte[1024];
        while (rlen >= 0 && size > 0) {
            rlen = session.getInputStream().read(buf, 0, (int) Math.min(size, 1024));
            size -= rlen;
            if (rlen > 0) {
                outputStream.write(buf, 0, rlen);
            }
        }

        outputStream.flush();
        outputStream.close();

        Intent intent = new Intent(CommunicationService.ACTION_RECEIVE_NEW_FILE);
        intent.putExtra(ConnectUtils.EXTRA_RECEIVE_FILE_PATH, file.getPath());
        this.mContext.get().sendBroadcast(intent);
        this.pollRequest();
        return newFixedLengthResponse(this.mContext.get().getString(R.string.upload_success_msg));
    }

    private class ReqInputStream extends FileInputStream {

        public ReqInputStream(File file) throws FileNotFoundException {
            super(file);
            managerRequest(System.currentTimeMillis() + REQ_DOWNLOAD);
        }

        @Override
        public void close() throws IOException {
            super.close();
            pollRequest();
        }
    }

}
