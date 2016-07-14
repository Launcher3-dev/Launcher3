package com.mxlibrary.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 12:00
 */
public class ConstantUtils {

    public static class Common {
        /**
         * 短信
         */
        public static final String SMMS = "mms";
        /**
         * 电话
         */
        public static final String CONTACTS = "contacts";
        /**
         * 通话记录
         */
        public static final String CONTACT = "contacts";
        /**
         * 短信内容
         */
        public static final String MMS = "mms";

    }

    public static class ShareKey {
        /**
         * sharedPreferences 文件名称
         */
        public static final String sSharedPreferencesKey = "com.android.launcher2.prefs";
    }

    public static final String KEY_FORMAT_DATE_YYMMDD = "YYYY-MM-DD";
    public static final String KEY_SHARE_DEEP_CLEAR = "YYYY-MM-DD";
    public static final String KEY_SHARE_KEY_DEEP_CLEAR = "YYYY-MM-DD";

    //时间常量
    public final static String FORMAT_DATE_YYMMDD = "yyyy-MM-dd";

    public final static String FORMAT_DATE_YYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";


    public final static String FORMAT_DATE_YYMMDD_HHMMSS = "yyyy-MM-dd-HH-mm-ss";


    public final static String SHARE_PERFERRENCE_CLICKEVENT = "click_event";

    public final static String SHARE_PERFERRENCE_APKENDTIME = "apk_end_time";

    public static class Cache {
        public static final String TAG = "LtpFileUtil";
        public final static String ROOT_DIR = Environment.getExternalStorageDirectory() + File.separator + ".xlauncher" + File.separator;
        public final static String CACHE_DIR = ROOT_DIR + "cache" + File.separator;
        public final static String WALLPAPER_CACHE_DIR = CACHE_DIR + "wallpaper/";
        public final static String BIG_WALLPAPER_CACHE_DIR = CACHE_DIR + "wallpaperbig/";
        public final static String DOWNLOAD_WALLPAPER_DIR = ROOT_DIR + "wallpaper" + File.separator;
        public final static String LIVE_WALLPAPER_DIR = ROOT_DIR + "livewallpaper" + File.separator;
        public final static String CACHE_FOLDER_HOTWORD = ROOT_DIR + "folder_hotword_cache" + File.separator;
        public final static String CACHE_FOLDER_DISPACTH_DIR = ROOT_DIR + "dispacth" + File.separator;
        public final static String CACHE_APP_PUSH_DIR = ROOT_DIR + "apppush" + File.separator;
        public final static String CACHE_ADVERTISEMENT_PIC_DIR = ROOT_DIR + File.separator + ".ad/" + File.separator;
        public final static String UUID_CACHE_DIR = ROOT_DIR + "uuid/";
        public final static String UUID_DIR = ROOT_DIR + "uuid" + File.separator;
        public static final String cache_dir = CACHE_DIR + "/search2/";
        public static final String CACHE_APP_CENTER = ROOT_DIR + "appcenter" + File.separator;
    }

    public static class Time {
        public final static int SEVEN_DAYS_HOURS = 7 * 24 * 60 * 60 * 1000;
        public final static int TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000;
        public final static int FOUR_HOURS = 4 * 60 * 60 * 1000;
        public final static int TWELVE_HOURS = 12 * 60 * 60 * 1000;
        public final static int ONE_HOURS = 60 * 60 * 1000;
        public final static long TWO_MINIUTE = 2 * 60 * 1000;
    }

}
