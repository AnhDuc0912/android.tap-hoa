package com.example.hango.api;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/api/similar-images")  // Thay bằng endpoint thực tế của bạn
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);

    @Multipart
    @POST("/api/similar-images")
    Call<ResponseWrapper> uploadImg(@Part MultipartBody.Part image);

    @GET("/api/get-products")
    Call<ProductsResponse> getProducts();

    @GET("api/get-products")
    Call<ProductsResponse> loadMoreProducts(@Query("offset") int offset);
}

