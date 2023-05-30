package com.file.transfer.core.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TransferModel {

    @SerializedName("downloadSize")
    public long downloadSize;

    @SerializedName("fileName")
    public String fileName;

    @SerializedName("downloadId")
    public long downloadId;

    @Expose(serialize = false)
    public int progress;

    @Expose(serialize = false)
    public String realPaths;

    @Expose(serialize = false)
    public int type;

}
