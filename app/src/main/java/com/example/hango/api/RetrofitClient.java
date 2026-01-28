package com.example.hango.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.229:5000/"; // ✅ nhớ dấu /

    private static Retrofit retrofit;

    // ✅ Tạo client với timeout cao
    private static OkHttpClient buildHttpClient() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY); // In log ra Logcat

        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // thời gian chờ kết nối (mặc định 10s)
                .readTimeout(120, TimeUnit.SECONDS)   // thời gian chờ đọc dữ liệu từ server
                .writeTimeout(120, TimeUnit.SECONDS)  // thời gian chờ ghi dữ liệu (upload file)
                .callTimeout(180, TimeUnit.SECONDS)   // tổng thời gian tối đa cho 1 request
                .retryOnConnectionFailure(true)
                .addInterceptor(log)
                .build();
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .serializeNulls()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(buildHttpClient()) // ✅ gắn client có timeout
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getInstance().create(ApiService.class);
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}
