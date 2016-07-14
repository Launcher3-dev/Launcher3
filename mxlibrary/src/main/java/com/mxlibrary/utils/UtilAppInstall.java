package com.mxlibrary.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 16:07
 */
public class UtilAppInstall {

    public static void downloadApk(Context context, String packageName,
                                   String downloadUrl) {
        // 如果是googleplay的应用直接跳转到googleplay
        if (isGooglePlayInstalled(context)) {
            try {
                moveToGooglePlay(context, packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(downloadUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Uri uri1 = Uri
                        .parse("http://dw4.cn.uptodown.com/dl/1436238969/ac3ae7666bd538552a0b9fd8c1e9966961a3d957/google-play-5-6-8-multi-android.apk");
                Intent intentUrl = new Intent(Intent.ACTION_VIEW, uri1);
                context.startActivity(intentUrl);
            }
        }
    }

    public static void openUrl(Context context, String url) {
        if (!TextUtils.isEmpty(url)) {
            Uri uri1 = Uri.parse(url);
            Intent intentUrl = new Intent(Intent.ACTION_VIEW, uri1);
            context.startActivity(intentUrl);
        }
    }

    /**
     * 打开应用市场
     *
     * @param mContext 上下文
     * @param pkgName  包名
     */
    public static void openMarket(final Context mContext, String pkgName) {
        try {
            if (isGooglePlayInstalled(mContext)) {
                moveToGooglePlay(mContext, pkgName);
            } else {
                openCNMarket(mContext, pkgName);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 打开国内市场
     *
     * @param mContext 上下文
     * @param pkgName  包名
     */
    public static void openCNMarket(final Context mContext, String pkgName) {
        try {
            Uri uri = Uri.parse("market://details?id=" + pkgName);
            Intent market = new Intent(Intent.ACTION_VIEW, uri);
            market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(market);
        } catch (Exception e) {
        }
    }

    /**
     * 判断GooglePlay的应用包是否存在
     */
    public static boolean isGooglePlayInstalled(Context mContext) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            if (info.activityInfo.applicationInfo.packageName.equals("com.android.vending") && info.activityInfo.name.equals("com.android.vending.AssetBrowserActivity")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开googelPlay市场
     *
     * @param pkgName 　包名
     *
     * @throws Exception
     */
    public static void moveToGooglePlay(Context mContext, String pkgName) throws Exception {
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        i.setComponent(new ComponentName("com.android.vending", "com.android.vending.AssetBrowserActivity"));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    /**
     * 根据包名判断应用是否安装
     *
     * @param context     上下文
     * @param packageName 包名
     *
     * @return 包含返回true，否则返回false
     */
    public static boolean checkApkExist(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
