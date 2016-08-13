package com.mxlibrary.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 15:52
 */
public class UtilDevice {

    private static String mSelfId;
    private static String mGooglePalyVersion;

    /**
     * 获取IMEI
     *
     * @return IMEI
     */
    public static String getIMEI(Context context) {
        try {
            TelephonyManager TelephonyMgr = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = TelephonyMgr.getDeviceId();
            if (deviceId == null) {
                String s = getSelfId(context);
                if (s == null) {
                    return "";
                } else {
                    return s;
                }
            } else
                return deviceId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取sn
     *
     * @return sn
     */
    public static String getSN(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String sn = tm.getSimSerialNumber();
            if (sn == null) {
                return "";
            } else {
                return sn;
            }
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 获取手机国家号码＋运营商代码
     *
     * @return simOperatorCode
     */
    public static String getSimOperatorCode(Context context) {
        TelephonyManager TelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return TelephonyMgr.getSimOperator();
    }

    public static String getCountryCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimCountryIso();
    }

    /**
     * 获取手机国家号码
     *
     * @return MCC
     */
    public static String getMCC(Context context) {
        TelephonyManager TelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String simOperator = TelephonyMgr.getSimOperator();
        if (!TextUtils.isEmpty(simOperator) && simOperator.length() > 3) {
            return simOperator.substring(0, 3);
        }
        return "";
    }

    /**
     * 获取手机运营商代码
     *
     * @return MNC
     */
    public static String getMNC(Context context) {
        TelephonyManager TelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String simOperator = TelephonyMgr.getSimOperator();
        if (!TextUtils.isEmpty(simOperator) && simOperator.length() > 3) {
            return simOperator.substring(3, simOperator.length());
        }
        return "";
    }

    /**
     * get network operator(网络运营商)
     *
     * @return NetWorkOperator
     */
    public static String getNetworkOperator(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkOperator();
    }

    /**
     * 获取Android ID
     *
     * @return AndroidID
     */
    public static String getAndroidID(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取手机型号
     *
     * @return PhoneModel
     */
    public static String getPhoneModel() {
        return Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return PhoneBrand
     */
    public static String getPhoneBrand() {
        return Build.BRAND;
    }

    /**
     * 获取系统当前设置的语言
     *
     * @return language
     */
    public static String getLanguage(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        return locale.getLanguage();
    }

    public static String getCountry(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        return locale.getCountry();
    }

    /**
     * 判断屏幕横竖屏，横屏是1，竖屏是2
     *
     * @return 横竖屏
     */
    public static int orientation(Context c) {
        Configuration cf = c.getResources().getConfiguration(); // 获取设置的配置信息
        int ori = cf.orientation; // 获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            return 1;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏
            return 2;
        }
        return 2;
    }

    /**
     * 获取Mac 地址
     *
     * @return Mac Address
     */
    public static String getMacAddress(Context context) {
        try {
            WifiManager wifi = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            String mac = info.getMacAddress();
            if (mac == null) {
                return "";
            }
            mac = mac.replaceAll(":", "");
            return mac.toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * app version code应用版本号
     *
     * @return Version Code
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * app version name应用版本名
     *
     * @return Version Name
     */
    public static String getVersionName(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static int getDisplayW(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getDisplayH(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    /**
     * app 报名
     *
     * @return package Name
     */
    public static String getPackageName(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return pi.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * app 名
     *
     * @return app name
     */
    public static String getAppName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getPackageName(context),
                    0);
            return (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String userAgent;

    /**
     * 获取UserAgent
     *
     * @return UserAgent
     */
    public static synchronized String getDefaultUserAgent_UI(final Context c) {
        if (userAgent == null) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(Context.class, WebView.class);
                        constructor.setAccessible(true);
                        try {
                            WebSettings settings = constructor.newInstance(c, null);
                            userAgent = settings.getUserAgentString();
                        } finally {
                            constructor.setAccessible(false);
                        }
                    } catch (Exception e) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) { // 2015-10-30.hugn.5.0.2系统会出现com.android.webkit找不到的问题，兼容处理
                            userAgent = "";
                        } else {
                            userAgent = new WebView(c).getSettings().getUserAgentString();
                        }
                    }
                }
            });
        }
        return userAgent;
    }

    /**
     * 获取UserAgent
     *
     * @return UserAgent
     */
    public static synchronized String getDefaultUserAgent_UI() {
        return userAgent;
    }

    public static String getPhoneNumber(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getLine1Number();
    }

    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取网络类型，WIFI网络则使用－1
     *
     * @param context 上下文
     *
     * @return Net Type
     *
     * @see TelephonyManager#NETWORK_TYPE_UNKNOWN
     * @see TelephonyManager#NETWORK_TYPE_GPRS
     * @see TelephonyManager#NETWORK_TYPE_EDGE
     * @see TelephonyManager#NETWORK_TYPE_UMTS
     * @see TelephonyManager#NETWORK_TYPE_CDMA
     * @see TelephonyManager#NETWORK_TYPE_EVDO_0
     * @see TelephonyManager#NETWORK_TYPE_EVDO_A
     * @see TelephonyManager#NETWORK_TYPE_1xRTT
     * @see TelephonyManager#NETWORK_TYPE_HSDPA
     * @see TelephonyManager#NETWORK_TYPE_HSUPA
     * @see TelephonyManager#NETWORK_TYPE_HSPA
     * @see TelephonyManager#NETWORK_TYPE_IDEN
     * @see TelephonyManager#NETWORK_TYPE_EVDO_B
     * @see TelephonyManager#NETWORK_TYPE_LTE
     * @see TelephonyManager#NETWORK_TYPE_EHRPD
     * @see TelephonyManager#NETWORK_TYPE_HSPAP
     */
    public static int getNetType(Context context) {
        int netType = -1;
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo == null) {
                return TelephonyManager.NETWORK_TYPE_UNKNOWN;
            }
            int nType = networkInfo.getType();
            if (nType == ConnectivityManager.TYPE_MOBILE) {
                netType = networkInfo.getSubtype();
            } else if (nType == ConnectivityManager.TYPE_WIFI) {
                netType = -1;
            }
        } catch (Exception e) {
            // Logger.e("DeviceUtil", "getNetType mothod fail !", e);
        }
        return netType;
    }

    public static String getSelfId(Context context) {
        if (mSelfId == null) {
            File installation = new File(Environment
                    .getExternalStorageDirectory().toString(),
                    "/.a/track_id.bin");
            try {
                if (!installation.exists()) {
                    mSelfId = writeInstallationFile(context, installation);
                } else {
                    mSelfId = readInstallationFile(installation);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mSelfId == null)
            return "";
        return mSelfId;
    }

    public static void setLastPostTime(Context context, long time) {
        File postTime = new File(Environment.getExternalStorageDirectory()
                .toString(), "/.a/post_time.bin");
        try {
            writeFile(context, postTime, time + "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getLastPostTime() {
        File timeFile = new File(Environment.getExternalStorageDirectory()
                .toString(), "/.a/post_time.bin");
        String timeStr = "0";
        try {
            if (timeFile.exists()) {
                timeStr = readInstallationFile(timeFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Long.valueOf(timeStr);
    }

    public static boolean isSDcardAvailable() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private static String readInstallationFile(File installation)
            throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(installation, "r");
        byte[] bs = new byte[(int) accessFile.length()];
        accessFile.readFully(bs);
        accessFile.close();
        return new String(bs);
    }

    private static String writeInstallationFile(Context context,
                                                File installation) throws IOException {
        UUID uuid = UUID.randomUUID();
        writeFile(context, installation, uuid.toString());
        return uuid.toString();
    }

    private static void writeFile(Context context, File file, String content)
            throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(content.getBytes());
        out.close();
    }

    /**
     * 获取Google Paly 的版本号
     *
     * @param context 上下文
     *
     * @return google play version
     */
    public static String getGooglePalyVersion(Context context) {
        if (!TextUtils.isEmpty(mGooglePalyVersion)) {
            List<PackageInfo> packageInfoList = context.getPackageManager()
                    .getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfoList) {
                if (packageInfo.packageName
                        .equalsIgnoreCase("com.android.vending")) {
                    mGooglePalyVersion = packageInfo.versionName;
                    break;
                }
            }
        }
        return mGooglePalyVersion;
    }

    /**
     * 获取Google Paly Service 的版本号
     *
     * @param context 上下文
     *
     * @return google play service version
     */
    public static String getGooglePalyServiceVersion(Context context) {
        String version = "";
        List<PackageInfo> packageInfoList = context.getPackageManager()
                .getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfoList) {
            if (packageInfo.packageName
                    .equalsIgnoreCase("com.google.android.gms")) {
                version = packageInfo.versionName;
                break;
            }
        }
        return version;
    }

    public static String getRealIp() {
        String localip = null;// 本地IP，如果没有配置外网IP则返回它
        String netip = null;// 外网IP
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            InetAddress ip = null;
            boolean finded = false;// 是否找到外网IP
            while (netInterfaces.hasMoreElements() && !finded) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {// 外网IP
                        netip = ip.getHostAddress();
                        finded = true;
                        break;
                    } else if (ip.isSiteLocalAddress()
                            && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                        localip = ip.getHostAddress();
                    }
                }
            }
            if (netip != null && !"".equals(netip)) {
                return netip;
            } else {
                return localip;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        return tz.getDisplayName(false, TimeZone.SHORT, Locale.ENGLISH);
    }

    public static void openBrowserUrl(Context c, String url) {
        if (url == null || c == null)
            return;
        try {
            Intent intent = new Intent("android.intent.action.VIEW",
                    Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo resolveInfo = c.getPackageManager().resolveActivity(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            // This is the default browser's pakagetName
            if (resolveInfo != null) {
                String packagetName = resolveInfo.activityInfo.packageName;
                intent.setClassName(packagetName, resolveInfo.activityInfo.name);
            }
            c.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Intent intent = new Intent("android.intent.action.VIEW",
                        Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(intent);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * 判断是否是平板
     *
     * @return
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static TelephonyManager getTelMan(Context context) {
        return (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * 获取运营商名字
     */
    public static String getOperatorName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String operator = telephonyManager.getSimOperator();
        if (operator != null) {
            if (operator.equals("46000") || operator.equals("46002")) {
                return "中国移动";
            } else if (operator.equals("46001")) {
                return "中国联通";
            } else if (operator.equals("46003")) {
                return "中国电信";
            }
        }
        return "未知";
    }

    public static ConnectivityManager getConnM(Context context) {
        return (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static WifiManager getWifiMan(Context context) {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * 判断网络是否可用
     *
     * @param context
     *
     * @return
     */
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo networkInfo;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return networkInfo != null && networkInfo.isAvailable()
                && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    public static int getAndroidOSVersion() {
        int sdkVersion;
        try {
            sdkVersion = Build.VERSION.SDK_INT;
        } catch (NumberFormatException e) {
            sdkVersion = 0;
        }

        return sdkVersion;
    }

    //获取运营商编码
    public static String getMobileVendor(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimOperator();
    }


    /**
     * 从系统内存信息文件中读取总的内存
     */
    public static int getTotalMemory(Context context) {
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine
                    .indexOf("MemTotal:"));
            br.close();
            long totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll(
                    "\\D+", ""));
            return (int) totalMemorySize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
