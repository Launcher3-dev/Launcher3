package com.mxlibrary.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 12:25
 */
public class UtilSharedPreferences {

    private static final String sShareKey = "share_leagoo";


    public static void saveStringData(Context context, String key, String data) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, data);
        editor.apply();
    }

    public static String getStringData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        return preferences.getString(key, "");
    }

    public static void saveBooleanData(Context context, String key, boolean data) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, data);
        editor.apply();
    }

    public static boolean getBooleanData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, false);
    }

    public static void saveIntData(Context context, String key, int data) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, data);
        editor.apply();
    }

    public static int getIntData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        return preferences.getInt(key, -1);
    }

    public static void saveLongData(Context context, String key, long data) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, data);
        editor.apply();
    }

    public static long getLongData(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(sShareKey, Context.MODE_PRIVATE);
        return preferences.getLong(key, -1);
    }

}
