package com.example.hango.api;
import com.example.hango.entitys.Product;
import com.example.hango.entitys.Category;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/api/similar-images")
    Call<ResponseWrapper> uploadImg(@Part MultipartBody.Part image);

    @POST("//api/add-product")
    Call<Product> addProduct(@Body Product product);

    @GET("api/get-categories")
    Call<CategoryResponse> getCategories();

}

