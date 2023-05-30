package com.file.transfer.core.model;

import androidx.annotation.IntDef;

@IntDef({
        BrowserType.OTHER,
        BrowserType.IMAGE,
        BrowserType.VIDEO,
        BrowserType.AUDIO,
        BrowserType.APK,
        BrowserType.PDF,
        BrowserType.WORD,
        BrowserType.EXCEL,
        BrowserType.PTT,
        BrowserType.ALBUM,
        BrowserType.DOUCUMENT_FILE,
        BrowserType.DOWNLOADED_FILE,
        BrowserType.HEADER,
        BrowserType.TXT,
        BrowserType.ZIP,
        BrowserType.RAR,
        BrowserType.RECENT,
        BrowserType.BOOKMARK,
        BrowserType.CLOUD,
        BrowserType.ARCHIVE,
        BrowserType.DIRECTORY,
})
public @interface BrowserType {
    int HEADER = -1;
    int OTHER = 0;
    int IMAGE = 1;
    int VIDEO = 2;
    int AUDIO = 3;
    int APK = 4;
    int PDF = 5;
    int WORD = 6;
    int EXCEL = 7;
    int PTT = 8;
    int TXT = 9;
    int ALBUM = 10;
    int DOUCUMENT_FILE = 11;
    int DOWNLOADED_FILE = 12;
    int ZIP = 13;
    int RAR = 14;
    int RECENT = 16;
    int BOOKMARK = 17;
    int CLOUD = 18;
    int ARCHIVE = 19;
    int DIRECTORY = 30;
}
