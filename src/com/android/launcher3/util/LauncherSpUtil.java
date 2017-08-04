package com.android.launcher3.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharePreference utils
 * <p>
 * Created by yuchuan on 02/08/2017.
 */

public class LauncherSpUtil {

    private static final String KEY_SP = "sp_mx";
    public static final String KEY_QSB = "sp_key_qsb";// 搜索
    public static final String KEY_ALL_APPS = "sp_key_all_apps";// 所有app图标是否显示
    public static final String KEY_PULL_DOWN_SEARCH = "sp_key_pull_down_search";// 下来搜索
    public static final String KEY_LIGHT_STATUS_BAR = "sp_key_light_status_bar";// 状态栏亮还是暗
    public static final String KEY_PAGE_LOOP = "sp_key_page_loop";// 滑动循环
    public static final String KEY_ALL_APPS_PULL_UP = "sp_key_all_apps_pull_up";// 向上滑动显示所有APP页面


    public static void saveStringData(Context context, String key, String data) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, data);
        editor.apply();
    }

    public static String getStringData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getString(key, "");
    }

    public static void saveBooleanData(Context context, String key, boolean data) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, data);
        editor.apply();
    }

    public static boolean getBooleanData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, false);
    }

    public static void saveIntData(Context context, String key, int data) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, data);
        editor.commit();
    }

    public static int getIntData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getInt(key, -1);
    }

    public static int getIntData(Context context, String key, int defaultValue) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getInt(key, defaultValue);
    }

    public static void saveLongData(Context context, String key, long data) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, data);
        editor.apply();
    }

    public static long getLongData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getLong(key, -1);
    }

    public static void saveFloatData(Context context, String key, float data) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(key, data);
        editor.apply();
    }

    public static float getFloatData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getFloat(key, -1.0f);
    }

    public static float getFloatData(Context context, String key, float defaultValue) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_SP, Context.MODE_PRIVATE);
        return preferences.getFloat(key, defaultValue);
    }


}
