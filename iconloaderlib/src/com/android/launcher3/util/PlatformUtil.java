package com.android.launcher3.util;

import android.content.pm.PackageInfo;
import android.os.Build;

/**
 * Created by yuchuan.gu
 * DATE 2022/1/25
 * TIME 17:22
 */
public final class PlatformUtil {

   public static long getVersion( PackageInfo info) {
      long infoVerson = 0;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
         infoVerson = info.getLongVersionCode();
      } else {
         infoVerson = info.versionCode;
      }
      return infoVerson;
   }

}

