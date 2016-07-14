package com.mxlibrary.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 16:09
 */
public class UtilAppInfo {

    public static String getMetaDataString(Context con, String key) {
        try {
            android.content.pm.ApplicationInfo mAppInfo = con
                    .getPackageManager().getApplicationInfo(
                            con.getPackageName(), PackageManager.GET_META_DATA);
            return mAppInfo.metaData.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getMetaDataInt(Context con, String key) {
        try {
            android.content.pm.ApplicationInfo mAppInfo = con
                    .getPackageManager().getApplicationInfo(
                            con.getPackageName(), PackageManager.GET_META_DATA);
            return mAppInfo.metaData.getInt(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取标题栏高度
     *
     * @param window 窗口对象
     *
     * @return 标题栏的高度
     * 开发者: 伍孝权
     * 时间：2015-7-22 下午5:22:14
     */
    public static int getTitleBarHeight(Window window) {
        if (window == null) {
            return 0;
        }
        final Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    /**
     * close soft input keyboard when click the white free space
     * <p>
     * 2014-11-14
     */
    public static void onHideSoftInput(Context context, View view) {
        if (view != null && view.getWindowToken() != null) {
            InputMethodManager mInputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * to check weather should show or hide the soft-keyboard
     * <p>
     * 2014-11-14
     *
     * @param v     the view that get the current focus
     * @param event
     *
     * @return
     */
    public static boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            return !(event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom);
        }
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditView上，和用户用轨迹球选择其他的焦点
        return false;
    }

}
