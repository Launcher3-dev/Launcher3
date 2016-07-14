package com.mxlibrary.utils;

import android.content.Context;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 16:11
 */
public class UtilDensity {


    public static float getDensity(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().density;
    }

    public static int dip2px(Context context, float dpValue) {
        return (int) (dpValue * getDensity(context) + 0.5F);
    }

    public static int px2dip(Context context, float pxValue) {
        return (int) (pxValue / getDensity(context) + 0.5F);
    }

    public static int getScreenWidth(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getApplicationContext().getResources().getDisplayMetrics().heightPixels;
    }

    public static String getResolution(Context context) {
        return getScreenWidth(context) + "X" + getScreenHeight(context);
    }
}
