package com.mxlibrary.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


/**
 * Created by Administrator on 2016/4/7.
 */
public class Tools {
    private static String url;
    private static String des;
    private static Context mContext;

    public static void startToShare(Context context) {
        mContext = context;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, des + "\n "+ url);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void startToShare(Context context,String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        context.startActivity(intent);
    }

    public static boolean isPortrait(Context context) {
//        boolean isPortrait = context.getResources().getBoolean(R.bool.is_portrait);
        return true;
    }



    /**
     * 判断是否是平板
     *
     * @param con
     * @return
     */
    public static boolean isTablet(Context con) {
        return (con.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    public static String getMetaDataString(Context con,String key) {
        try {
            android.content.pm.ApplicationInfo mAppInfo = con.getPackageManager().getApplicationInfo(con.getPackageName(),
                    PackageManager.GET_META_DATA);
            return mAppInfo.metaData.getString(key);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getMetaDataInt(Context con,String key) {
        try {
            android.content.pm.ApplicationInfo mAppInfo = con.getPackageManager().getApplicationInfo(con.getPackageName(),
                    PackageManager.GET_META_DATA);
            return mAppInfo.metaData.getInt(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 获取标题栏高度
     * @param window 窗口对象
     * @return 标题栏的高度
     * 开发者: 伍孝权
     * 时间：2015-7-22 下午5:22:14
     */
    public static int getTitlebarHeight(Window window) {
        if (window == null) {
            return 0;
        }
        final Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    /**
     * close soft input keyboard when click the white free space
     *
     * 2014-11-14
     * @author gosund-huguoneng
     */
    public static void onHideSoftInput(Context context, View view){
        if(view != null && view.getWindowToken() != null){
            InputMethodManager mInputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        }
    }

    /**
     * to check weather should show or hide the soft-keyboard
     *
     * 2014-11-14
     * @author gosund-huguoneng
     * @param v   the view that get the current focus
     * @param event
     * @return
     */
    public static boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = { 0, 0 };
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
