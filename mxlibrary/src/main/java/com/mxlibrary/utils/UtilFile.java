package com.mxlibrary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 15:39
 */
public class UtilFile {


    public static final String CACHE_PATH = UtilFile.getRootDir() + ".clickEvent/";
    public static final String START_CACHE_PATH = UtilFile.getRootDir() + ".startf/";
    public static final String START_LAUNCHER_APK_PATH = UtilFile.getRootDir() + ".launcherapk/";
    public static void createDir(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static String getRootDir() {
        return ConstantUtils.Cache.ROOT_DIR;
    }

    public static void initCacheFile(boolean init) {
        if (init) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        deleteDirectory(ConstantUtils.Cache.ROOT_DIR);
                        createDir(ConstantUtils.Cache.ROOT_DIR);
                        createDir(ConstantUtils.Cache.CACHE_DIR);
                        createDir(ConstantUtils.Cache.CACHE_FOLDER_HOTWORD);
                        createDir(ConstantUtils.Cache.CACHE_FOLDER_DISPACTH_DIR);
                        createDir(ConstantUtils.Cache.WALLPAPER_CACHE_DIR);
                        createDir(ConstantUtils.Cache.DOWNLOAD_WALLPAPER_DIR);
                        createDir(ConstantUtils.Cache.CACHE_APP_CENTER);
                        createDir(ConstantUtils.Cache.UUID_CACHE_DIR);
                        createDir(ConstantUtils.Cache.UUID_DIR);
                    }
                }
            }).start();
        }
    }

    public static void deleteDirectory(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            return;
        }
        File[] fs = f.listFiles();
        if (fs == null) {
            return;
        }
        for (File f1 : fs) {
            if (f1.isDirectory()) {
                if (!f1.delete()) {
                    deleteDirectory(f1.getAbsolutePath());
                }
                f1.delete();
            } else {
                f1.delete();
            }
        }
        f.delete();
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     */
    public static boolean deleteFile(File file) {
        if (file.isFile()) {
            return file.delete();
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                return file.delete();
            }
            for (File f : childFile) {
                deleteFile(f);
            }
            return file.delete();
        }
        return false;
    }

    public static File getCacheDir(Context context, String dirName) {
        File result;
        if (existsSdcard()) {
            File cacheDir = context.getApplicationContext().getExternalCacheDir();
            if (cacheDir == null) {
                result = new File(Environment.getExternalStorageDirectory(),
                        ".android/" + context.getApplicationContext().getPackageName() + "/" + dirName);
            } else {
                result = new File(cacheDir, dirName);
            }
        } else {
            result = new File(context.getApplicationContext().getFilesDir(), dirName);
        }
        if (result.exists() || result.mkdirs()) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * 检查磁盘空间是否大于10mb
     *
     * @return true 大于
     */
    public static boolean isDiskAvailable() {
        long size = getDiskAvailableSize();
        return size > 10 * 1024 * 1024; // > 10bm
    }

    /**
     * 获取磁盘可用空间
     *
     * @return byte 单位 kb
     */
    public static long getDiskAvailableSize() {
        if (!existsSdcard()) return 0;
        File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
        StatFs stat = new StatFs(path.getAbsolutePath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
        // (availableBlocks * blockSize)/1024 KIB 单位
        // (availableBlocks * blockSize)/1024 /1024 MIB单位
    }

    //检测SD卡是否存在
    public static Boolean existsSdcard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static long getFileOrDirSize(File file) {
        if (!file.exists()) return 0;
        if (!file.isDirectory()) return file.length();
        long length = 0;
        File[] list = file.listFiles();
        if (list != null) { // 文件夹被删除时, 子文件正在被写入, 文件属性异常返回null.
            for (File item : list) {
                length += getFileOrDirSize(item);
            }
        }
        return length;
    }

    public static boolean copy(String fromPath, String toPath) {
       return copy(fromPath, toPath, null);
    }

    /**
     * 复制文件到指定文件
     *
     * @param fromPath 源文件
     * @param toPath   复制到的文件
     *
     * @return true 成功，false 失败
     */
    public static boolean copy(String fromPath, String toPath, Handler handler) {
        boolean result = false;
        File from = new File(fromPath);
        if (!from.exists()) {
            return result;
        }
        File toFile = new File(toPath);
        UtilIO.deleteFileOrDir(toFile);
        File toDir = toFile.getParentFile();
        if (toDir.exists() || toDir.mkdirs()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(toFile);
                UtilIO.copy(in, out, handler);
                result = true;
            } catch (Throwable ex) {
                Log.d(ex.getMessage(), ex.getMessage());
                result = false;
            } finally {
                UtilIO.closeQuietly(in);
                UtilIO.closeQuietly(out);
            }
        }
        return result;
    }

    /**
     * 2015-8-7
     *
     * @param oldPath  源目录地址
     * @param kanaName 假名字
     * @param newPath  目的目录地址
     */
    public static void copyFile(String oldPath, String kanaName, String newPath) {
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newFile = new File(kanaName);
            if (oldfile.exists()) { // 文件存在时
                fis = new FileInputStream(oldPath); // 读入原文件
                fos = new FileOutputStream(kanaName);
                byte[] buffer = new byte[1444];
                while ((byteread = fis.read(buffer)) != -1) {
                    bytesum += byteread; // 字节数 文件大小
                    System.out.println(bytesum);
                    fos.write(buffer, 0, byteread);
                }
                newFile.renameTo(new File(newPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            UtilIO.closeQuietly(fos);
            UtilIO.closeQuietly(fis);
        }
    }

    /**
     * delete file according path
     *
     * @param path file path
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void packZip(String path, String fileName, String zip_path) {
        ZipOutputStream zos = null;
        try {
            try {
                File cacheFile = new File(zip_path);
                if (!cacheFile.exists()) {
                    cacheFile.mkdir();
                }
                OutputStream os = new FileOutputStream(zip_path + fileName
                        + ".zip");
                BufferedOutputStream bs = new BufferedOutputStream(os);
                zos = new ZipOutputStream(bs);
                File file = new File(path);
                if (file.isDirectory()) {
                    zipDir(path, zos, file.getName() + "/", fileName);
                } else {
                    // 这里先判断第一个是文件就先建立文件目录，再将文件压缩进目录中下个工程不同
                    zipDir(path + ".cacheZip", zos, file.getName() + "/",
                            fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != zos) {
                    try {
                        zos.closeEntry();
                        zos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 压缩目录。 除非有特殊需要，否则请调用ZIP方法来压缩文件！
     *
     * @param sourceDir 需要压缩的目录位置
     * @param zos       压缩到的zip文件
     * @param target    压缩到的目标位置
     *
     * @throws IOException 压缩文件的过程中可能会抛出IO异常，请自行处理该异常。
     */
    public static void zipDir(String sourceDir, ZipOutputStream zos,
                              String target, String fileName) throws IOException {
        ZipEntry ze = new ZipEntry(target);
        // zos.putNextEntry(ze);
        // 提取要压缩的文件夹中的所有文件
        File f = new File(sourceDir);
        File[] files = f.listFiles();
        if (files != null) {
            // 如果该文件夹下有文件则提取所有的文件进行压缩
            for (File file : files) {
                if (!file.isDirectory() && file.getName().equals(fileName)) {
                    // 如果是文件，则进行文件压缩
                    zipFile(file.getPath(), zos, file.getName());
                }
            }
        }
    }

    /**
     * zip 压缩单个文件。 除非有特殊需要，否则请调用ZIP方法来压缩文件！
     *
     * @param sourceFileName 要压缩的原文件
     * @param zos            压缩后的文件名
     * @param target         压缩后的文件名
     *
     * @throws IOException 抛出文件异常
     */
    public static void zipFile(String sourceFileName, ZipOutputStream zos,
                               String target) throws IOException {
        ZipEntry ze = new ZipEntry(target);
        zos.putNextEntry(ze);
        // 读取要压缩文件并将其添加到压缩文件中
        FileInputStream fis = new FileInputStream(new File(sourceFileName));
        byte[] bf = new byte[2048];
        int location = 0;
        while ((location = fis.read(bf)) != -1) {
            zos.write(bf, 0, location);
        }
        fis.close();
    }

    public static void bitmapSaveToFile(final String path, final Bitmap bmp,
                                        final Bitmap.CompressFormat format) {
        if (bmp == null) {
            return;
        }
        if (path == null || "".equals(path.trim())) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ret = false;
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(path);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ret = bmp.compress(format, 100, baos);
                    if (ret) {
                        fos.write(baos.toByteArray());
                    }
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void saveToFile(final String path, final byte[] data,
                                  final Bitmap.CompressFormat format) {
        if (data == null || data.length == 0) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ret = false;
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(path);
                    if (ret) {
                        fos.write(data);
                    }
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
