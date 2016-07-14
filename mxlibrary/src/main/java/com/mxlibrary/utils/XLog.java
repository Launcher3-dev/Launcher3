package com.mxlibrary.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yuchuan
 * DATE 16/1/29
 * TIME 19:55
 */
public class XLog {
    //加载使数据过滤
    public static final String TAG_GU = "TAG_GU----- ";
    // 是否打印log，打包apk要设置为false
    private static final boolean sEnablePrint = true;

    /**
     * 打印日志时获取当前的程序文件名、行号、方法名 输出格式为：[FileName | LineNumber | MethodName]
     *
     * @return tag
     */
    public static String getTag() {
        if (!sEnablePrint) {
            return "";
        }
        //StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[3];
        return "[" + traceElement.getFileName() + " | "
                + traceElement.getLineNumber() + " | "
                + traceElement.getMethodName() + "]";

    }

    public static void v(String tag, String log) {
        if (sEnablePrint) {
            Log.v(tag, log);
        }
    }

    public static void i(String tag, String log) {
        if (sEnablePrint) {
            Log.i(tag, log);
        }
    }

    public static void e(String tag, String log) {
        if (sEnablePrint) {
            Log.e(tag, log);
        }
    }

    public static void d(String tag, String log) {
        if (sEnablePrint) {
            Log.d(tag, log);
        }
    }

    public static void w(String tag, String log) {
        if (sEnablePrint) {
            Log.w(tag, log);
        }
    }

    public static void w(String tag, String log, Exception e) {
        if (sEnablePrint) {
            Log.e(tag, log, e);
        }
    }

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm ss");

    /**
     * 将log写入手机SdCard中
     *
     * @param log    log内容
     * @param append 是否追加log
     */
    public static synchronized void writeLog(final String log, final boolean append) {
        if (!sEnablePrint) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    return;
                }
                BufferedWriter writer = null;
                try {
                    File file = new File(Environment.getExternalStorageDirectory(), "mayalog.txt");
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    StringBuffer date = new StringBuffer();
                    date.append(format.format(new Date())).append("~~");
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append)));
                    writer.write(date.toString());
                    writer.write(log);
                    writer.write("\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 将log及异常写入手机sdCard
     *
     * @param ex     异常
     * @param log    log信息
     * @param append 是否追加
     */
    public static synchronized void writeLog(final Exception ex, final String log, final boolean append) {
        if (!sEnablePrint) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                PrintStream writer = null;
                File file = new File(Environment.getExternalStorageDirectory(), "log.txt");
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    StringBuffer date = new StringBuffer();
                    date.append(format.format(new Date())).append("~~");
                    writer = new PrintStream(new FileOutputStream(file, append), true);
                    writer.print(date.toString());
                    writer.print(log);
                    ex.printStackTrace(writer);
                    writer.print("\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        }).start();
    }
}
