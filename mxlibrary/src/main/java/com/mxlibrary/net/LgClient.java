package com.mxlibrary.net;

import com.mxlibrary.net.imp.RetrofitService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 初始化网络请求客户端,可以自己设置超时时间,可以用默认超时时间
 * <p/>
 * Created by yuchuan
 * DATE 3/23/16
 * TIME 10:52
 */
public class LgClient {
    private RetrofitService mRetrofitService;

    LgClient(OkHttpClient client, Retrofit.Builder builder) {
        if (client != null) {
            builder.client(client);
        }
        this.mRetrofitService = builder.build().create(RetrofitService.class);
    }

    public static final class LgBuilder {
        OkHttpClient client;
        OkHttpClient.Builder okBuilder;
        Retrofit.Builder retrofitBuilder;
        private Map<String, Retrofit.Builder> map;

        public LgBuilder() {
            map = new HashMap<>();
        }

        /**
         * 设置根url
         *
         * @param baseUrl 网址
         *
         * @return
         */
        public LgBuilder setBaseUrl(String baseUrl) {
            if (map != null && map.containsKey(baseUrl)) {
                retrofitBuilder = map.get(baseUrl);
            } else {
                retrofitBuilder = createBuilder();
                retrofitBuilder.baseUrl(baseUrl);
                retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(baseUrl, retrofitBuilder);
            }
            return this;
        }

        /**
         * 设置连接超时
         *
         * @param connectTimeout 超时时间
         *
         * @return
         */
        public LgBuilder setConnectTimeout(int connectTimeout) {
            if (okBuilder == null) {
                okBuilder = new OkHttpClient.Builder();
            }
            okBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * 设置读写超时
         *
         * @param timeout 超时时间
         *
         * @return
         */
        public LgBuilder setTimeout(int timeout) {
            if (okBuilder == null) {
                okBuilder = new OkHttpClient.Builder();
            }
            okBuilder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * 添加转换器
         *
         * @param factory 转换器
         *
         * @return
         */
        public LgBuilder addConverterFactory(Converter.Factory factory) {
            if (retrofitBuilder == null) {
                throw new IllegalArgumentException ("Not set Base Url");
            }
            retrofitBuilder.addConverterFactory(factory);
            return this;
        }

        public LgBuilder addCallAdapterFactory(CallAdapter.Factory factory) {
            if (retrofitBuilder == null) {
                throw new IllegalArgumentException ("Not set Base Url");
            }
            retrofitBuilder.addCallAdapterFactory(factory);
            return this;
        }

        public LgClient build() {
            if (okBuilder != null) {
                client = okBuilder.build();
            }
            if (retrofitBuilder == null) {
                retrofitBuilder = createBuilder();
            }
            return new LgClient(client, retrofitBuilder);
        }

        private Retrofit.Builder createBuilder() {
            return new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create());
        }

    }

    public RetrofitService getThemeService() {
        return mRetrofitService;
    }


}
