package com.mxlibrary.net.imp;

import com.mxlibrary.net.UrlConstants;
import com.mxlibrary.net.bean.ServerTimeBean;
import com.mxlibrary.net.bean.SuccessBean;
import com.mxlibrary.net.bean.ThemeBean;
import com.mxlibrary.net.bean.UpdataInfo;
import com.mxlibrary.net.bean.WallpaperListBean;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;

/**
 * 网络数据请求接口,所有网络请求的方法均在此添加
 * <p/>
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 22:36
 */
public interface RetrofitService {
//
//    //String BASE_URL = "http://192.168.199.234:8080/leagoo/";
      String BASE_URL = "http://data.me2tek.com:39090/leagooapi/";
//    String BASE_URL = "http://192.168.2.200:39090/leagooapi/";
//    String BASE_URL_THEME = "http://192.168.2.169:39090/leagooapi/";
//    String BASE_URL_THEME = "http://192.168.2.200:8089/theme/api/";
      String BASE_URL_THEME = "http://theme.me2tek.com:8081/theme/api/";
//    String BASE_URL = "http://data.mayamobi.com:39090/leagoo/";
//    String BASE_URL = "http://192.168.2.169:39090/leagooapi/";

    /**
     * 主题列表请求接口方法:
     * GET中的参数为BaseUrl后面的不同的部分
     *
     * @param start 开始位置
     * @param end   结束位置
     *
     * @return 返回获取数据的回调对象
     */
    @GET(UrlConstants.URL_THEME)
    Call<List<ThemeBean>> getThemeList(@Query("start") int start, @Query("end") int end);


    /**
     * POST方法数据请求,Call中的参数为对象形式,由于加了转换器,所以使用对象形式
     * "leagoo/saveRecorder.action?imeiNum=113"
     *
     * @param map 请求中要发送的数据表
     *
     * @return 返回需要的对象
     */
    @FormUrlEncoded
    @POST("saveRecorder.action?")
    Call<SuccessBean> updateLgInfo(@FieldMap Map<String, String> map);

    /**
     * 获取改设备是否一提交过资料
     * "sendImeiMsg?imeiNum=110"
     *
     * @param imei 请求参数
     *
     * @return 发送过为:1;未发送为:0
     */
    @FormUrlEncoded
    @POST("checkSendImeiMsg.action?")
    Call<SuccessBean> checkSendImeiMsg(@Field("imeiNum") String imei);


    /**
     * POST方法数据请求,更新版本号
     *
     * @param map 请求中要发送的数据表
     *
     * @return 返回需要的对象
     */
    @FormUrlEncoded
    @POST("upgradeVersion.action?")
    Call<UpdataInfo> checkVersion(@FieldMap Map<String, String> map);

    /**
     * 下载文件
     *
     * @param fileId 参数
     *
     * @return
     */
    @GET("/files/{fileId}")
    @Headers({"Content-Type: image/jpeg"})
    Response getFile(@Path("fileId") int fileId);

    /**
     * <<<<<<< HEAD
     * 下载文件
     *
     * @param param
     *
     * @return
     */
    @Streaming
    @GET("url")
    Call<ResponseBody> getFile(@QueryMap Map<String, String> param);


    /**
     * 上传一个文件
     *
     * @param file
     *
     * @return
     */
    @Multipart
    @POST("user/edit")
    Call<SuccessBean> upload(@Part("image\";filename=\"file.jpg") RequestBody file);

    /**
     * 同时上传多个文件
     *
     * @param map
     *
     * @return
     */
    @Multipart
    @POST("user/edit")
    Call<SuccessBean> upload(@PartMap Map<String, RequestBody> map);

    /**
     * 主题列表请求接口方法:
     * GET中的参数为BaseUrl后面的不同的部分
     *
     * @return 返回获取数据的回调对象
     */
    @GET("upload/upgrade/{apkurl}")
    Call<ResponseBody> getFile(@Path("apkurl") String apkurl);

    /**
     * POST方法数据请求,Call中的参数为对象形式,由于加了转换器,所以使用对象形式
     * "leagoo/saveRecorder.action?imeiNum=113"
     *
     * @param map 请求中要发送的数据表
     *
     * @return 返回需要的对象
     */
    @FormUrlEncoded
    @POST("saveRecorder.action?")
    Call<SuccessBean> uploadInformation(@FieldMap Map<String, String> map);


    /**
     * 应用点击上报接口
     */
    @FormUrlEncoded
    @POST("saveAppEventRecorder.action?")
    Call<SuccessBean> saveAppEventRecorder(@FieldMap Map<String, String> map);


    /**
     * 时间间隔接口
     */
    @FormUrlEncoded
    @POST("getServerTime.action?")
    Call<ServerTimeBean> getServerTime(@FieldMap Map<String, String> map);


    /**
     * 获取壁纸列表
     *
     * @param map 请求参数集合
     *
     * @return 壁纸对象列表
     */
    @FormUrlEncoded
    @POST("wallpaper/getWallpaperList?")
    Call<WallpaperListBean> getWallpaperList(@FieldMap Map<String, String> map);

}
