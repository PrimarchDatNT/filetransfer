package com.file.transfer.core.util;

import android.os.SystemClock;
import android.view.View;

public abstract class OnceClick implements View.OnClickListener {

    private static final long MIN_CLICK_INTERVAL = 500;

    private long lastClickTime;

    public abstract void onSingleClick(View v);

    @Override
    public final void onClick(View v) {
        long currentClickTime = SystemClock.uptimeMillis();
        long elapsedTime = currentClickTime - lastClickTime;
        this.lastClickTime = currentClickTime;

        if (elapsedTime <= MIN_CLICK_INTERVAL) return;

        this.onSingleClick(v);
    }
}
