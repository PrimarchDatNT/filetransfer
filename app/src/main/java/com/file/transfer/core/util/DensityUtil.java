package com.file.transfer.core.util;

import android.content.Context;

public class DensityUtil {

    public static int widthPixels(Context context) {
        if (context == null || context.getResources() == null) {
            return 720;
        }
        return context.getResources().getDisplayMetrics().widthPixels;
    }

   public static int heightPixels(Context context) {
        if (context == null || context.getResources() == null) {
            return 1280;
        }
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
