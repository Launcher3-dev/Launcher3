package com.mxlibrary.net;

import android.os.Environment;
import android.util.Log;

import com.mxlibrary.net.bean.Download;
import com.mxlibrary.net.bean.ServerTimeBean;
import com.mxlibrary.net.bean.SuccessBean;
import com.mxlibrary.net.bean.UpdataInfo;
import com.mxlibrary.net.bean.WallpaperListBean;
import com.mxlibrary.net.imp.ImpRequest;
import com.mxlibrary.net.imp.RetrofitService;
import com.mxlibrary.utils.UtilIO;
import com.mxlibrary.utils.XLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 网络请求工具类,所有请求均在此调用
 * 使用方法:
 * Retrofit.getInstance()
 * .setBaseUrl(baseUrl)
 * .setConnectTimeout(10 * 1000)
 * .setTimeout(10 * 1000)
 * .build()
 * .updateDeviceInfo();
 * <p>
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 22:50
 */
public class RetrofitUtil {

    public static final int RETROFITUTIL_BASE_URL = 0;
    public static final int RETROFITUTIL_THEME_URL = 1;

    private static LgClient sLgClient;
    private static RetrofitService sRetrofitService;
    private LgClient.LgBuilder mLgBuilder;
    private HashMap<String, Call> mRequestMap;

    public RetrofitUtil() {
        mRequestMap = new HashMap<>();
        mLgBuilder = new LgClient.LgBuilder();
    }

    //获取单例
    public static RetrofitUtil getInstance() {
        return RetrofitUtilHolder.sRetrofitUtil;
    }


    private static class RetrofitUtilHolder {
        private static final RetrofitUtil sRetrofitUtil = new RetrofitUtil();
    }

    public RetrofitUtil setBaseUrl(String baseUrl) {
        if (mLgBuilder == null) {
            mLgBuilder = new LgClient.LgBuilder();
        }
        mLgBuilder.setBaseUrl(baseUrl);
        return this;
    }

    public RetrofitUtil setBaseUrl(int type) {
        if (mLgBuilder == null) {
            mLgBuilder = new LgClient.LgBuilder();
        }
        mLgBuilder.setBaseUrl(getBaseUrl(type));
        return this;
    }

    /**
     * 根据类型获取对应的url
     * @param type 类型:RETROFITUTIL_BASE_URL,RETROFITUTIL_THEME_URL
     * @return
     */
    private String getBaseUrl(int type) {
        switch (type) {
            case RETROFITUTIL_BASE_URL:
                return RetrofitService.BASE_URL;
            case RETROFITUTIL_THEME_URL:
                return RetrofitService.BASE_URL_THEME;
            default:
                return RetrofitService.BASE_URL;
        }
    }

    public RetrofitUtil build() {
        sLgClient = mLgBuilder.build();
        sRetrofitService = sLgClient.getThemeService();
        return this;
    }

    /**
     * 设置连接超时
     *
     * @param connectTimeout 超时时间
     *
     * @return
     */
    public RetrofitUtil setConnectTimeout(int connectTimeout) {
        if (mLgBuilder == null) {
            mLgBuilder = new LgClient.LgBuilder();
        }
        mLgBuilder.setConnectTimeout(connectTimeout);
        return this;
    }

    /**
     * 设置读写超时
     *
     * @param timeout 超时时间
     *
     * @return
     */
    public RetrofitUtil setTimeout(int timeout) {
        if (mLgBuilder == null) {
            mLgBuilder = new LgClient.LgBuilder();
        }
        mLgBuilder.setTimeout(timeout);
        return this;
    }

    public void updateDeviceInfo(Map<String, String> map) {
        Call<SuccessBean> call = sRetrofitService.updateLgInfo(map);
        call.enqueue(new Callback<SuccessBean>() {
            @Override
            public void onResponse(Call<SuccessBean> call, Response<SuccessBean> response) {

            }

            @Override
            public void onFailure(Call<SuccessBean> call, Throwable t) {

            }
        });
    }


    public void upload() {
        File file = new File("file/abc.jpg");
        RequestBody body = RequestBody.create(MediaType.parse("image/png"), file);
        Map<String, RequestBody> map = new HashMap<>();
        map.put("image\";filename=\"" + file.getName(), body);
        Call<SuccessBean> call = sRetrofitService.upload(map);
        call.enqueue(new Callback<SuccessBean>() {
            @Override
            public void onResponse(Call<SuccessBean> call, Response<SuccessBean> response) {
                if (response != null && response.isSuccessful()) {
                    SuccessBean bean = response.body();
                }
            }

            @Override
            public void onFailure(Call<SuccessBean> call, Throwable t) {

            }
        });
    }

    public static void saveBytesToFile(byte[] bytes, String path) {
        FileOutputStream fileOutputStream = null;
        try {
            Log.e("EventRecorderSaveApp", "EventRecorderSaveApp path====" + path);
            fileOutputStream = new FileOutputStream(path);
            fileOutputStream.write(bytes);
        } catch (FileNotFoundException e) {
            Log.e("EventRecorderSaveApp", "EventRecorderSaveApp FileNotFoundException====" + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("EventRecorderSaveApp", "EventRecorderSaveApp IOException====" + e);
            e.printStackTrace();
        } finally {
            UtilIO.closeQuietly(fileOutputStream);
        }
    }

    /**
     * 添加请求进行
     * <p>
     * 管理
     *
     * @param key  键
     * @param call 请求对象
     */
    private void addCallToMap(String key, Call call) {
        if (mRequestMap == null) {
            mRequestMap = new HashMap<>();
        }
        mRequestMap.put(key, call);
    }

    /**
     * 根据键值删除请求
     *
     * @param key 键
     */
    private void removeCallFromMap(String key) {
        if (mRequestMap == null || mRequestMap.isEmpty()) {
            return;
        }
        if (mRequestMap.containsKey(key)) {
            mRequestMap.remove(key);
        }
    }

    /**
     * 根据对象删除请求
     *
     * @param call 请求对象
     */
    private void removeCallFromMap(Call call) {
        if (mRequestMap == null || mRequestMap.isEmpty()) {
            return;
        }
        if (mRequestMap.containsValue(call)) {
            mRequestMap.remove(call);
        }
    }

    /**
     * 版本号更新
     */

    public void checkVersions(Map<String, String> map, final ImpRequest impRequest) {
        Call<UpdataInfo> call = sRetrofitService.checkVersion(map);
        call.enqueue(new Callback<UpdataInfo>() {
            @Override
            public void onResponse(Call<UpdataInfo> call, Response<UpdataInfo> response) {
                if (impRequest != null) {
                    Log.e("mUpdataInfos","mUpdataInfos   call  ==="+ call);
                    Log.e("mUpdataInfos","mUpdataInfos   response  ==="+ response);
                    Log.e("mUpdataInfos","mUpdataInfos   response.isSuccessful()  ==="+ response.isSuccessful());
                    if (response.isSuccessful()) {
                        Log.e("mUpdataInfos","mUpdataInfos   response.body()  ==="+ response.body());
                        impRequest.onSuccess(response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<UpdataInfo> call, Throwable t) {
                Log.e("mUpdataInfos", " mUpdataInfos Throwable===" + t);
                if (impRequest != null) {
                    impRequest.onFailure();
                }
            }
        });
    }


    /**
     * 下载更新
     */
    public void downloadFile(final String apkurl,final ImgetRealData mImgetRealData) {
        Log.e("mNotificationManager"," mNotificationManager.downloadFile==");
        if (sRetrofitService == null) {
            return;
        }
        Log.e("mNotificationManager"," mNotificationManager.sRetrofitService != null==");
        Call<ResponseBody> call = sRetrofitService.getFile(apkurl);
        Log.e("mNotificationManager","mNotificationManager sRetrofitService.getFile(apkurl)==");
        Log.e("mNotificationManager","mNotificationManager sRetrofitService.call=="+call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                final Response<ResponseBody> mResponse = response;
                Log.e("mNotificationManager"," mNotificationManager.downloadFile= ==========onResponse=  response======"+response);
                Log.e("mNotificationManager"," mNotificationManager.downloadFile= ==========mResponse.body()======="+mResponse.body());
                if (mImgetRealData == null || mResponse.body()==null) {
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileOutputStream out = null;
                        Download mDownload = new Download();
                        double mDouble = 0.0;
                        File file = new File(Environment.getExternalStorageDirectory(), apkurl);
                        try {
                            InputStream input = mResponse.body().byteStream();
                            long length = response.body().contentLength();
                            out = new FileOutputStream(file);
                            int bufferSize = 1024;
                            byte[] buffer = new byte[bufferSize];
                            int len = 0;
                            float mCount = 0;
                            while ((len = input.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                                mCount += len;
                                double temp = mCount / len / 1024 / 10;
                                if (temp >= mDouble) {
                                    mDouble += 0.1;
                                    int load = (int) (mCount / length * 100);

                                    mDownload.setDownloadProgress(load);
                                    mDownload.setDownloadcompleted(false);
                                    mImgetRealData.getRsalData(mDownload);
                                }
                                //函数调用handler发送信息

                            }
                            mDownload.setDownloadcompleted(true);

                            if (out == null) {
                                return;
                            }
                            mImgetRealData.getRsalData(mDownload);

                        } catch (IOException e) {
                            Log.e("mNotificationManager"," mNotificationManager.downloadFile= ==========IOException======="+e);
                            e.printStackTrace();
                        } finally {
                            UtilIO.closeQuietly(out);
                        }
                    }
                }).start();
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("mNotificationManager"," mNotificationManager.Throwable=="+t);
            }
        });


    }

    public interface ImgetRealData {
        void getRsalData(Download mDownload);
    }

    public void informationUpdate(Map<String, String> map, final ImpRequest impRequest) {
        Call<SuccessBean> call = sRetrofitService.uploadInformation(map);
        call.enqueue(new Callback<SuccessBean>() {
            @Override
            public void onResponse(Call<SuccessBean> call, Response<SuccessBean> response) {
                if (impRequest != null) {
                    if (response.isSuccessful()) {
                        impRequest.onSuccess(response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<SuccessBean> call, Throwable t) {
                if (impRequest != null) {
                    impRequest.onFailure();
                }
            }
        });
    }


    public void saveAppEven(Map<String, String> map, final ImpRequest impRequest) {
        Call<SuccessBean> call = sRetrofitService.saveAppEventRecorder(map);
        call.enqueue(new Callback<SuccessBean>() {
            @Override
            public void onResponse(Call<SuccessBean> call, Response<SuccessBean> response) {
                if (impRequest != null) {
                    if (response.isSuccessful()) {
                        impRequest.onSuccess(response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<SuccessBean> call, Throwable t) {
                if (impRequest != null) {
                    impRequest.onFailure();
                }
            }
        });
    }

    public void getTimeServer(Map<String, String> map, final ImpRequest impRequest) {
        Call<ServerTimeBean> call = sRetrofitService.getServerTime(map);
        call.enqueue(new Callback<ServerTimeBean>() {
            @Override
            public void onResponse(Call<ServerTimeBean> call, Response<ServerTimeBean> response) {
                if (impRequest != null) {
                    if (response.isSuccessful()) {
                        impRequest.onSuccess(response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<ServerTimeBean> call, Throwable t) {
                if (impRequest != null) {
                    impRequest.onFailure();
                }
            }
        });
    }

    /**
     * 获取壁纸列表
     *
     * @param map        参数
     * @param impRequest 接口
     */
    public void getWallpaperList(Map<String, String> map, final ImpRequest impRequest) {
        Call<WallpaperListBean> call = sRetrofitService.getWallpaperList(map);
        call.enqueue(new Callback<WallpaperListBean>() {
            @Override
            public void onResponse(Call<WallpaperListBean> call, Response<WallpaperListBean> response) {
                if (impRequest != null && response.isSuccessful()) {
                    impRequest.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<WallpaperListBean> call, Throwable t) {
                XLog.e(XLog.getTag(), XLog.TAG_GU + t.getMessage());
                if (impRequest != null) {
                    impRequest.onFailure();
                }
            }
        });
    }

}
