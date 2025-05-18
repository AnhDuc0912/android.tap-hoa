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
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/api/similar-images")
    Call<ResponseWrapper> uploadImg(@Part MultipartBody.Part image);

    @GET("/api/get-products")
    Call<ProductsResponse> getProducts();

    @GET("api/get-products")
    Call<ProductsResponse> loadMoreProducts(@Query("offset") int offset);

    @POST("api/load-more-similar")
    Call<ResponseWrapper> loadMoreSimilarProducts(@Query("offset") int offset);
}

