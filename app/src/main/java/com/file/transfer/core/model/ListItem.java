package com.file.transfer.core.model;

import android.text.TextUtils;

import java.io.File;

public class ListItem extends BaseFileItem {


    public ListItem(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        this.path = path;
        if (exists()) {
            File file = new File(path);
            this.name = file.getName();
            this.dir = file.isDirectory();
            this.size = file.length();
            this.mtime = file.lastModified();
        }
    }

    public boolean exists() {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    public String getParent() {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        return new File(path).getParent();
    }

}
