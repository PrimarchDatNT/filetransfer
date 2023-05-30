package com.file.transfer.core.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Objects;

public class BaseFileItem implements Serializable {

    public static Collator collator;
    public String name;
    public String path;
    public boolean selected;
    public long mtime;
    public boolean dir;
    public long size;
    public int type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public boolean isDir() {
        return dir;
    }

    public void setDir(boolean dir) {
        this.dir = dir;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseFileItem that = (BaseFileItem) o;
        return mtime == that.mtime &&
                dir == that.dir &&
                size == that.size &&
                type == that.type &&
                name.equals(that.name) &&
                path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, mtime, dir, size, type);
    }

    @NonNull
    @Override
    public String toString() {
        return "BaseFileItem{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", selected=" + selected +
                ", mtime=" + mtime +
                ", dir=" + dir +
                ", size=" + size +
                ", type=" + type +
                '}';
    }

    public static class SortFiles implements Comparator<BaseFileItem> {
        boolean arcFirst;
        int sortMode;

        public SortFiles(int sortMode) {
//            SharedPreferences sharedPref = SystemF.getSharedPref();
            this.sortMode = sortMode;
//            this.arcFirst = sharedPref.getBoolean(AppKeyConstant.PREFS_ARCFIRST, true);
            this.arcFirst = true;
        }

        public int nameCmp(String str, String str2) {
            if (ListItem.collator == null) {
                ListItem.collator = Collator.getInstance();
                ListItem.collator.setStrength(0);
            }
            return ListItem.collator.compare(str, str2);
        }

        public int compare(BaseFileItem listItem, BaseFileItem listItem2) {
            if (listItem == null || listItem2 == null) {
                return -1;
            }
            try {
                boolean equals = listItem.name.equals("..");
                boolean equals2 = listItem2.name.equals("..");
                if ((equals && !equals2) || (listItem.dir && !listItem2.dir)) {
                    return -1;
                }

                if ((!equals && equals2) || (!listItem.dir && listItem2.dir)) {
                    return 1;
                }

//                if (this.arcFirst && !listItem.dir) {
//                    boolean z = false;
//                    boolean z2 = PathF.isArcName(listItem.name) || listItem.name.endsWith(".rev");
//                    if (PathF.isArcName(listItem2.name) || listItem2.name.endsWith(".rev")) {
//                        z = true;
//                    }
//                    if (z2 && !z) {
//                        return -1;
//                    }
//                    if (!z2 && z) {
//                        return 1;
//                    }
//                }

                if (this.sortMode == 1) {
                    return nameCmp(listItem.name, listItem2.name);
                } else if (this.sortMode == 2) {
                    long j = listItem.size;
                    long j2 = listItem2.size;
                    if (j == j2) {
                        return nameCmp(listItem.name, listItem2.name);
                    }
                    return j > j2 ? -1 : 1;
                } else {
                    long j3 = listItem.mtime;
                    long j4 = listItem2.mtime;
                    if (j3 == j4) {
                        return nameCmp(listItem.name, listItem2.name);
                    }
                    return j3 > j4 ? -1 : 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }
}
