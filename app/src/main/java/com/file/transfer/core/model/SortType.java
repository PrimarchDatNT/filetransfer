package com.file.transfer.core.model;

import androidx.annotation.IntDef;

@IntDef({SortType.SORT_NAME,
        SortType.SORT_SIZE,
        SortType.SORT_DATE,
        SortType.SORT_TYPE,
        SortType.SORT_BY_FOLDER_SIZE,
        SortType.SORT_BY_LOG_INSERT
})
public @interface SortType {
    int SORT_NAME = 1;

    int SORT_SIZE = 2;

    int SORT_DATE = 3;

    int SORT_TYPE = 4;

    int SORT_BY_FOLDER_SIZE = 5;

    int SORT_BY_LOG_INSERT = 6;

}
