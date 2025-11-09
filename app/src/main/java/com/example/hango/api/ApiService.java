package com.example.hango.api;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ========== SEARCH ==========
    @Multipart
    @POST("search/image")
    Call<ApiListResponse<Item>> searchImage(
            @Part MultipartBody.Part file,
            @Part("k") RequestBody k,
            @Part("threshold") RequestBody threshold,
            @Part("apply_rerank") RequestBody applyRerank
    );

    @POST("search/text_with_image")
    Call<ApiListResponse<Item>> searchTextWithImage(@Body Map<String, Object> body);

    @GET("search/similar-skus")
    Call<ApiPagedResponse<Item>> getSimilarSkus(
            @Query("q") String query,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("threshold") double threshold
    );

    // ========== SKU ==========
    @GET("api/skus/view")
    Call<ApiListResponse<Item>> getSkusView(@Query("ids") String idsCsv);

    @GET("api/skus/{id}/view")
    Call<ApiSingleResponse<Item>> getSkuView(@Path("id") long skuId);

    @GET("api/skus")
    Call<ApiListResponse<Item>> getSkusList(
            @Query("brand_id") Long brandId,
            @Query("category_id") Long categoryId,
            @Query("q") String q
    );

    // ========== CART ==========
    @GET("cart/view")
    Call<ApiListResponse<Item>> getCartView(@Query("user_id") long userId);

    @GET("cart")
    Call<ApiListResponse<Item>> getCartRaw(@Query("user_id") long userId);

    // ========== EVENTS (NEW) ==========
    @POST("events/candidate_action")
    Call<ApiOkResponse> sendCandidateAction(@Body CandidateActionRequest body);
}