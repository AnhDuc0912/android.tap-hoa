package com.example.hango.api;
import com.example.hango.products.Product;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {

    @Multipart
    @POST("/search/image")
    Call<ResponseWrapper> uploadImg(@Part MultipartBody.Part image);

    @Multipart
    @POST("/search/image")
    Call<SearchImageResponse> searchByImage(
            @Part MultipartBody.Part file,     // tên field phải là "file"
            @Part("k") RequestBody k           // số lượng kết quả (vd 20)
    );

    @GET("/api/get-products")
    Call<ProductsResponse> getProducts();

    @GET("api/get-products")
    Call<ProductsResponse> loadMoreProducts(@Query("offset") int offset);

    @POST("api/load-more-similar")
    Call<ResponseWrapper> loadMoreSimilarProducts(@Query("offset") int offset);

    @GET("api/get-categories")
    Call<CatResponse> getCategories();

    @Multipart
    @POST("/api/add-product")
    Call<ResponseBody> uploadProduct(
            @Part MultipartBody.Part image,
            @Part("product_name") RequestBody productName,
            @Part("price") RequestBody price,
            @Part("category_id") RequestBody categoryId,
            @Part("unit") RequestBody unit
    );
}

