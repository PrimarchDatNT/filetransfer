package com.file.transfer.core.server;

import androidx.annotation.IntDef;

@IntDef({
        TransferType.DEFAULT,
        TransferType.WIFI_SHARE,
        TransferType.HOTSPOT_SHARE,
        TransferType.RECEIVE,
})
public @interface TransferType {

    int DEFAULT = 0;

    int WIFI_SHARE = 1;

    int HOTSPOT_SHARE = 2;

    int RECEIVE = 3;
}
