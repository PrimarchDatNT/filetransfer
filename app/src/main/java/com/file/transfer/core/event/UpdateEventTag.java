package com.file.transfer.core.event;

import androidx.annotation.IntDef;

@IntDef(value = {UpdateEventTag.TAG_SFP_UPDATE_STATE
        , UpdateEventTag.TAG_UPDATE_LOCAL_DATA
        , UpdateEventTag.TAG_UPDATE_CLOUD_DATA
        , UpdateEventTag.TAG_UPDATE_RENAMED_FILE
        , UpdateEventTag.TAG_UPDATE_RECENT_NEW_FILE
})
public @interface UpdateEventTag {

    int TAG_SFP_UPDATE_STATE = 1;
    int TAG_UPDATE_LOCAL_DATA = 2;
    int TAG_UPDATE_CLOUD_DATA = 3;
    int TAG_UPDATE_RENAMED_FILE = 4;
    int TAG_UPDATE_RECENT_NEW_FILE = 5;

}
