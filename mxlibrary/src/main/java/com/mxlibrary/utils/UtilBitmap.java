package com.mxlibrary.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * 图片处理工具:缩放,Drawable转Bitmap,Bitmap转Drawable,图片是否一致,高斯模糊
 *
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 12:08
 */
public class UtilBitmap {

    /**
     * 图片缩放
     *
     * @param width  最终宽度
     * @param height 最终高度
     * @param source 源图片
     *
     * @return 缩放后的图片
     */
    public static Bitmap scale(int width, int height, Bitmap source) {

        if (source == null) {
            throw new NullPointerException("bitmap is null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must > 0");
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        float widthScale = width / (source.getWidth() * 1.f);
        float heightScale = height / (source.getHeight() * 1.f);
        Matrix matrix = new Matrix();
        matrix.postScale(widthScale, heightScale, source.getWidth() / 2f, source.getHeight() / 2f);
        canvas.drawBitmap(source, matrix, null);
        return result;
    }

    /**
     * Drawable 转 Bitmap
     *
     * @param d
     *
     * @return
     */
    public static Bitmap drawableConvertBitmap(Drawable d) {
        try {
            if (d != null) {
                int width = d.getIntrinsicWidth();
                int height = d.getIntrinsicHeight();
                d.setBounds(0, 0, width, height);
                Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas();
                canvas.setBitmap(result);
                d.draw(canvas);
                canvas.setBitmap(null);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Bitmap 转 Drawable
     *
     * @param bmp
     *
     * @return
     */
    public static Drawable bitmapConvertDrawable(Context context, Bitmap bmp) {
        if (bmp == null) {
            return null;
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    /**
     * 得取数据判断是否一致
     *
     * @return boolean
     */
    public static boolean isSamePicture(String src, String dst) {
        if (src == null || src.isEmpty() || dst == null || dst.isEmpty()) {
            Log.w("notification", "Path cannot be null");
            return false;
        }
        boolean isSame = false;
        try {
            int byteCount = 20;
            long byteOffset1 = 30;
            byte[] srcBuff0 = new byte[byteCount];
            byte[] srcBuff1 = new byte[byteCount];
            byte[] dstBuff0 = new byte[byteCount];
            byte[] dstBuff1 = new byte[byteCount];
            RandomAccessFile srcFile = new RandomAccessFile(src, "r");
            RandomAccessFile dstFile = new RandomAccessFile(dst, "r");
            Log.w("UtilBitmap", "srcFile.length():" + srcFile.length() + ";dstFile.length():" + dstFile.length());
            if (srcFile.length() != dstFile.length()) {
                srcFile.close();
                dstFile.close();
                Log.w("notification", "isSamePicture is false");
                return false;
            }
            srcFile.seek(srcFile.length() - byteOffset1);
            srcFile.read(srcBuff0);
            dstFile.seek(dstFile.length() - byteOffset1);
            dstFile.read(dstBuff0);
            srcFile.seek(srcFile.length() / 2);
            srcFile.read(srcBuff1);
            dstFile.seek(dstFile.length() / 2);
            dstFile.read(dstBuff1);
            if (Arrays.equals(srcBuff0, dstBuff0) && Arrays.equals(srcBuff1, dstBuff1)) {
                isSame = true;
                Log.w("notification", "isSamePicture is true");
            }
            srcFile.close();
            dstFile.close();
        } catch (IOException e) {
            Log.w("notification", "error! not file");
            return false;
        }
        return isSame;
    }

    /**
     * 加载所有屏幕的高斯模糊图
     * <p>
     * 2015-01-08
     *
     * @param bkg
     * @param view return
     */
    public static Bitmap scaleBlurBitmap(Bitmap bkg, View view) {
        if (bkg == null) return null;
        if (view == null || view.getMeasuredWidth() == 0 || view.getMeasuredHeight() == 0) {
            return null;
        }
        //需要缩小的等级，在代码中我把bitmap的尺寸缩小到原图的1/8。因为这个bitmap在模糊处理时会先被缩小然后再放大，所以在我的模糊算法中就不用radius这个参数了，所以把它设成2。
        float scaleFactor = 10;
        float radius = 15;//模糊度
        //接着需要创建bitmap，这个bitmap比最后需要的小八倍。
        Bitmap overlay = Bitmap.createBitmap((int) ((view.getMeasuredWidth() > 150 ? (view.getMeasuredWidth() - 130) : view.getMeasuredWidth()) / scaleFactor),
                (int) ((view.getMeasuredHeight() > 500 ? (view.getMeasuredHeight() - 500) : view.getMeasuredHeight()) / scaleFactor), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.translate(-view.getLeft() / scaleFactor, -view.getTop() / scaleFactor);
        canvas.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        // Paint提供了FILTER_BITMAP_FLAG标示，这样的话在处理bitmap缩放的时候，就可以达到双缓冲的效果，模糊处理的过程就更加顺畅了。
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bkg, 0, 0, paint);
        return overlay = doBlurBitmap(overlay, (int) radius, true);
    }

    /**
     * 模糊处理操作，这次的图片小了很多，幅度也降低了很多，所以模糊过程非常快。
     * <p>
     * 2015-01-08
     */
    public static Bitmap doBlurBitmap(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }
        if (radius < 1) {
            return (null);
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];
                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }

    /**
     * 将View转成图片
     *
     * @return
     */
    public static Bitmap getBitmapFromView(Context context, final View cView, Bitmap result, Bitmap wallpaper) {
        final View view = cView;
        if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
            return null;
        }
        if (result == null) {
            return null;
        }
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(wallpaper, 0, 0, null);
        view.draw(canvas);
        canvas.setBitmap(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                destroyViewDrawingCache(view);
            }
        }).start();
        return result;
    }

    private static void destroyViewDrawingCache(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                destroyViewDrawingCache(child);
            }
        }
        view.destroyDrawingCache();
    }

    public static Bitmap createBlurSourceBitmap(int width, int height, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setAlpha(128);
        paint.setColor(color);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bmp);
        canvas.drawRect(new Rect(0, 0, width, height), paint);
        canvas.setBitmap(null);
        return bmp;
    }

    public static Bitmap decodeBitmapFromFile(String path, int width, int height) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, op);
            op.inSampleSize = calculateInSampleSize(op, width, height);
            op.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(path, op);
        } catch (OutOfMemoryError e) {
            System.gc();
            bitmap = null;
        }
        Bitmap result = null;
        if (bitmap != null) {
            int x = (bitmap.getWidth() - width) / 2;
            int y = bitmap.getHeight() - height;
            if (y < 0 || x < 0) {
                return bitmap;
            }
            result = Bitmap.createBitmap(bitmap, x, y, width, height);
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            float reqWidth, float reqHeight) {
        final int width = options.outWidth;
        float widthRadio = (float) width / reqWidth;
        return (int) widthRadio;
    }

    /**
     * save wall-paper bitmap as local file
     * <p>
     * 2014-12-20
     *
     * @param wpm    墙纸管理器
     * @param bitmap 当前被设为壁纸的图片
     */
    public static String saveWallpaperToLocal(WallpaperManager wpm, Bitmap bitmap, int wallpaperSize) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e("launcher_debug", "wallpaper bitmap is recycled");
            return null;
        }
        String filename = "wallpaper_" + (wallpaperSize - 1) + ".jpg";//因为selector占据弟一个位置，命名参考wallpapers.xml
        Log.e("launcher_debug", "filename = " + filename);
        writeFileToLocal(getLocalWallPaperDir(), "/" + filename, bitmap);
        boolean sameImage = compareImage(filename, getLocalWallPaperDir());
        Log.e("launcher_debug", "sameImage:" + sameImage);
        if (sameImage) {
            File file = new File(getLocalWallPaperDir(), filename);
            if (file != null && file.exists()) {
                Log.e("launcher_debug", " delete file");
                file.delete();
            }
        } else {
            String path = createWallPaperThumbNail(bitmap, getLocalWallPaperDir() /*+ "/small"*/, "/wallpaper_" + (wallpaperSize - 1) + "_small.jpg");
            bitmap.recycle();
            bitmap = null;
            return path;
        }
        return null;
    }

    /**
     * 判断是否有重复图片
     * <p>
     * 2014-12-22
     *
     * @param image1
     * @param path
     *
     * @return true :got duplicated image, other wise false
     */
    public static boolean compareImage(final String image1, String path) {
        boolean result = false;
        File srcFile = new File(path, image1);
        File targetFile = new File(path);
        if (targetFile != null && targetFile.exists()) {
            File[] items = targetFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File filename) {
                    return !filename.getName().equals(image1);
                }
            });
            if (items.length > 0) {
                for (File item : items) {
                    boolean isSame = UtilBitmap.isSamePicture(item.getAbsolutePath(), srcFile.getAbsolutePath());
                    if (isSame) {
                        Log.e("launcher_debug", "same file:" + item.getAbsolutePath() + ";src file:" + srcFile.getAbsolutePath());
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 创建大图片的缩略图,并将图片写入指定目录
     * <p>
     * 2014-12-20
     *
     * @param bitmap
     * @param path
     */
    public static String createWallPaperThumbNail(Bitmap bitmap, String path, String suffix) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        Bitmap thumb = ThumbnailUtils.extractThumbnail(bitmap, 152, 152);
        boolean result = writeFileToLocal(path, suffix, thumb);
        if (result) {
            Log.d("BitmapUtils", "createWallPaperThumbNail：" + path + suffix);
            return path + suffix;
        } else {
            return null;
        }
    }

    /**
     * 将图片文件写入指定目录
     * <p>
     * 2014-12-20
     *
     * @param path
     * @param filename
     * @param bitmap
     */
    public static boolean writeFileToLocal(String path, String filename, Bitmap bitmap) {
        File file = new File(path);
        if (!file.exists())
            file.mkdir();
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file.getPath() + filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * 获取本地壁纸原图历史记录目录
     * <p>
     * 2014-12-20
     *
     * @return
     */
    public static String getLocalWallPaperDir() {
        String path = null;
        try {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Xwallpaper";
        } catch (NoSuchMethodError e) {
            Log.e("BitmapUtils", "no method getExternalStorageAndroidDataDir");
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/Xwallpaper";
        }
        Log.e("BitmapUtils", "file dir:" + path);
        return path;
    }

}
