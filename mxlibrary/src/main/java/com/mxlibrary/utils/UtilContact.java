package com.mxlibrary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;

/**
 *
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 12:13
 */
public class UtilContact {

    /**
     * 获得电话组建的信息
     *
     * @param context   上下文
     * @param pkgName   包名
     * @param className 类名
     *
     * @return boolean
     */
    public static boolean getContacs(Context context, String pkgName, String className) {
        String mContacts = getSharedPreferences(context).getString(ConstantUtils.Common.CONTACTS, "");
        return !TextUtils.isEmpty(mContacts) && mContacts.contains(pkgName)
                && mContacts.contains(className) && mContacts.contains(":" + ConstantUtils.Common.CONTACT);
    }

    /**
     * 返回短信组建的信息
     *
     * @param context   上下文
     * @param pkgName   包名
     * @param className 类名
     *
     * @return boolean
     */
    public static boolean getMmsString(Context context, String pkgName, String className) {
        String mMms = getSharedPreferences(context).getString(ConstantUtils.Common.SMMS, "");
        return !TextUtils.isEmpty(mMms) && mMms.contains(pkgName)
                && mMms.contains(":" + ConstantUtils.Common.MMS)
                && mMms.contains(className);
    }

    /**
     * 返回共享参数
     *
     * @return
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ConstantUtils.ShareKey.sSharedPreferencesKey, Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    /**
     * 检测是否是拨号或者短信应用
     *
     * @param context   上下文对象
     * @param pkgName   包名
     * @param className 类名
     *
     * @return true 是拨号或短信应用， false 反之
     * 开发者: 伍孝权
     * 时间：2015-7-22 下午4:29:15
     */
    public static boolean checkIsMmsOrDialerApplication(Context context, String pkgName, String className) {
        final SharedPreferences preferences = getSharedPreferences(context);
        final String mMms = preferences.getString(ConstantUtils.Common.SMMS, "");
        if (!TextUtils.isEmpty(mMms) && mMms.contains(pkgName)
                && mMms.contains(":" + ConstantUtils.Common.MMS)
                && mMms.contains(className)) {
            return true;
        }
        final String mContacts = preferences.getString(ConstantUtils.Common.CONTACTS, "");
        return !TextUtils.isEmpty(mContacts) && mContacts.contains(pkgName)
                && mContacts.contains(className) && mContacts.contains(":" + ConstantUtils.Common.CONTACT);
    }

    /**
     * 保存电话短信标识符
     *
     * @param defaultPackageName 包名
     * @param defaultClassName   类名
     * @param appType2           应用的类型
     */
    public static void storeMmsContacts(Context context, String defaultPackageName, String defaultClassName, String appType2) {
        if (TextUtils.isEmpty(defaultClassName)
                || TextUtils.isEmpty(defaultPackageName)
                || TextUtils.isEmpty(appType2)) {
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(ConstantUtils.ShareKey.sSharedPreferencesKey, Context.MODE_PRIVATE);
        String str = defaultPackageName + ":" + defaultClassName + ":" + appType2;
        //电话应用程序
        if (appType2.equals(ConstantUtils.Common.CONTACTS)) {
            sp.edit().putString(ConstantUtils.Common.CONTACTS, str).apply();
        } else if (appType2.equals(ConstantUtils.Common.SMMS)) {
            //短信应用
            sp.edit().putString(ConstantUtils.Common.SMMS, str).apply();
        }
    }

    /**
     * 获取未读电话的条数
     *
     * @param context 上下文
     *
     * @return
     */
    public static int getMissCallCount(final Context context) {
        int missCount = 0;
        Cursor cursor = null;
        try {
            String[] select = new String[]{"number", "date", "type", "new"};
            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, select, "type=? and new=?",
                    new String[]{"3", "1"}, null);
            if (null != cursor) {
                missCount = cursor.getCount();
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return missCount;
    }

    /**
     * 获取未读短信条数
     *
     * @param mContext 上下文
     *
     * @return
     */
    public static int getUnreadSmsCount(Context mContext) {
        int result = 0;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Uri.parse("content://sms"), null, "type = 1 and read = 0",
                    null, null);
            if (cursor != null) {
                result = cursor.getCount();
                cursor.close();
            }
        } catch (Exception e) {
        } finally {
            UtilIO.closeQuietly(cursor);
        }
        return result;
    }

}
