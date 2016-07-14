package com.mxlibrary.utils;

import android.text.TextUtils;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 15:37
 */
public class UtilFilterPackage {

    public static String[] filterPackage = {"com.asus.launcher",
            "home.solo.launcher.free", "com.apusapps.launcher",
            "com.zeroteam.zerolauncher", "com.afast.launcher",
            "com.yaoo.qlauncher", "com.teslacoilsw.launcher",
            "com.kukool.iosapp.kulauncher", "com.zuikuai.home",
            "com.kuaizhuomian.launcher", "com.buzzpia.aqua.launcher",
            "com.moxiu.launcher", "com.gau.go.launcherex",
            "com.nd.android.pandahome2", "com.dianxinos.dxhome",
            "com.example.uephone.launcher", "com.tsf.shell",
            "com.nd.android.smarthome", "com.baidu.launcher",
            "com.qihoo360.launcher", "com.tencent.qlauncher",
            "com.baoruan.launcher2", "com.lenovo.launcher",
            "com.hola.launcher", "com.anddoes.launcher", "com.amigo.navi",
            "com.guobi.winguo.hybrid", "com.shafa.launcher",
            "com.codoon.easyuse", "telecom.mdesk", "com.campmobile.launcher",
            "com.gtp.launcherlab", "com.mobilewindow", "com.ailk.insight",
            "com.fhhr.launcherEx1", "com.nearme.launcher",
            "com.cooeeui.brand.turbolauncher", "com.baiyi_mobile.launcher",
            "com.coco.launcher", "com.xsg.launcher", "com.uprui.launcher.ios",
            "com.fhhr.launcherEx", "com.eztech.kylinlauncher"};

    public static boolean isIncludePackage(String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            for (String pack : filterPackage) {
                if (pack.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
