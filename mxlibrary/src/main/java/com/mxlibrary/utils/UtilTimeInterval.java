package com.mxlibrary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 时间间隔获取工具
 * Created by gyc.
 * DATE 3/22/16
 * TIME 11:51
 */
public class UtilTimeInterval {

    /**
     * 是否需要更新或者获取数据
     *
     * @param context      上下文
     * @param key          保存时间的键值
     * @param timeInterval 需要更新或者获取数据的时间间隔
     *
     * @return 是否需要更新或者时间间隔
     */
    public static boolean isNeedUpdate(Context context, String shareKey, String key, int timeInterval) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(shareKey, Context.MODE_PRIVATE);
        boolean auto = false;
        String simpleDateFormat = getSimpleDateFormat().format(new Date());
        Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========simpleDateFormat===="+simpleDateFormat);
        String autoDate = sharedPrefs.getString(key, "");
        Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========autoDate===="+autoDate);
        if (!"".equals(autoDate)) {
            try {
                Date date = getSimpleDateFormat().parse(autoDate);
                Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========date===="+date);
                Date now = new Date();
                long dateTime = date.getTime();
                Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========dateTime===="+dateTime);
                long nowTime = now.getTime();
                Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========nowTime===="+nowTime);
                Log.e("LauncherSettingActivity","LauncherSettingActivity ========nowTime - dateTime===="+(nowTime - dateTime));
                if ((nowTime - dateTime) > timeInterval) {
                    auto = true;
                }
            } catch (Exception e) {
                Log.e("LauncherSettingActivity","LauncherSettingActivity ========isNeedUpdate==========Exception===="+e);
                e.printStackTrace();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(key, simpleDateFormat);
                editor.apply();
            }
        } else {
            auto = true;
        }
        return auto;
    }

    public static SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat(ConstantUtils.FORMAT_DATE_YYMMDDHHMMSS);
    }



    public static void saveDownloadLtpAppCenterDataTime(Context context, String shareKey, String key) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(shareKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, getSimpleDateFormat().format(new Date()));
        editor.apply();
    }

    /**
     * 保存一键清理显示次数
     *
     * @param context 上下文
     * @param times   次数
     */
    public static void saveDeepClearShowAdTimes(Context context, int times) {
        SharedPreferences pre = context.getSharedPreferences(ConstantUtils.KEY_SHARE_DEEP_CLEAR, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt(ConstantUtils.KEY_SHARE_KEY_DEEP_CLEAR, times);
        editor.apply();
    }

    //获取深度清理弹出次数
    public static int getDeepClearShowAdTimes(Context context) {
        SharedPreferences pre = context.getSharedPreferences(ConstantUtils.KEY_SHARE_DEEP_CLEAR, Context.MODE_PRIVATE);
        return pre.getInt(ConstantUtils.KEY_SHARE_KEY_DEEP_CLEAR, -1);
    }

    public static int getRandomIndex(int size) {
        Random random = new Random();
        return random.nextInt(size);
    }



    /**
     * 获取时间间隔是否已到
     * @param timeInterval
     * @return
     */
    public static boolean getTimeInterval(Context context, int timeInterval){
        Log.e("app_event","app_event   getTimeInterval====" +timeInterval);
        timeInterval=timeInterval * UtilData.SRCOND;
//        timeInterval=1 * 1000;
        SimpleDateFormat format = new SimpleDateFormat(ConstantUtils.FORMAT_DATE_YYMMDDHHMMSS);
        String autoDate = UtilSharedPreferences.getStringData(context,ConstantUtils.SHARE_PERFERRENCE_CLICKEVENT);
        if(!"".equals(autoDate)){
            try {
                Date date = format.parse(autoDate);
                Date  now = new Date();
                long dateTime = date.getTime();
                long nowTime = now.getTime();
                if( (nowTime - dateTime) > (timeInterval)) {
                    String simpleDateFormat = new SimpleDateFormat(ConstantUtils.FORMAT_DATE_YYMMDDHHMMSS).format(new Date());
                    UtilSharedPreferences.saveStringData(context,ConstantUtils.SHARE_PERFERRENCE_CLICKEVENT,simpleDateFormat);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String simpleDateFormat = new SimpleDateFormat(ConstantUtils.FORMAT_DATE_YYMMDDHHMMSS).format(new Date());
                UtilSharedPreferences.saveStringData(context,ConstantUtils.SHARE_PERFERRENCE_CLICKEVENT,simpleDateFormat);
            }
        } else {
            String simpleDateFormat = new SimpleDateFormat(ConstantUtils.FORMAT_DATE_YYMMDDHHMMSS).format(new Date());
            UtilSharedPreferences.saveStringData(context,ConstantUtils.SHARE_PERFERRENCE_CLICKEVENT,simpleDateFormat);
        }
        return false;
    }

}
