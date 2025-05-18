package com.example.hango.api;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/api/similar-images")  // Thay bằng endpoint thực tế của bạn
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
}

