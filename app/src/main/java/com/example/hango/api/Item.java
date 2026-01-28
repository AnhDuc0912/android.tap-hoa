package com.example.hango.api;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Item {
    // IDs
    @SerializedName(value="sku_id",    alternate={"skuId"})    public Long skuId;
    @SerializedName(value="sku_text_id", alternate={"skuTextId"}) public Long skuTextId;

    // Names / brand
    @SerializedName(value="sku_name",  alternate={"skuName"})  public String skuName;
    @SerializedName(value="brand_name",alternate={"brandName"})public String brandName;

    // Display name server-side (nếu có)
    @SerializedName("product_name")    public String productName;

    // Image
    @SerializedName(value="image_path",alternate={"imagePath"})public String imagePath;

    // Description (server chuẩn hoá)
    @SerializedName(value="description", alternate={"descriptionText"})
    public String description;

    // Fallbacks (có thể có trong /search/image cũ)
    @SerializedName("text")            public String text;
    @SerializedName("ocr_text")        public String ocrText;

    // Search scores
    @SerializedName("score")           public Double score;
    @SerializedName("similarity")      public Double similarity;     // nếu backend còn trả
    @SerializedName("dist")            public Double dist;           // nếu cần hiển thị
    @SerializedName("re_rank_score")   public Double reRankScore;    // có thể có
    @SerializedName("combined_score")  public Double combinedScore;  // có thể có

    // Features (debug/hiển thị badge)
    @SerializedName("candidate_features")
    public Map<String, Object> candidateFeatures;

    // Giá + số lượng (nếu dùng ở cart)
    @SerializedName("price")           public Long price;
    @SerializedName("unit_price")
    public Long unitPrice;

    @SerializedName("selling_price")
    public Long sellingPrice;

    @SerializedName("quantity")        public Integer quantity;

    // ===== Helpers dùng ở UI =====
    public String getDisplayName() {
        if (notEmpty(productName)) return productName.trim();
        if (notEmpty(skuName))     return skuName.trim();
        return "Không rõ";
    }

    public String getDisplayDescription() {
        if (notEmpty(description)) return description.trim();
        if (notEmpty(text))        return text.trim();
        if (notEmpty(ocrText))     return ocrText.trim();
        if (notEmpty(brandName))   return brandName.trim();
        return "";
    }

    public String getImageUrl() {
        return imagePath; // nếu backend trả relative path, nhớ prepend baseUrl ở chỗ Glide
    }

    public Long getSkuIdSafe() {
        return skuId;
    }

    public Double getSimilarityPct() {
        Double v = score != null ? score : similarity;
        return (v == null) ? null : Math.max(0, Math.min(100, v * 100.0));
    }

    public int getQtyOrDefault() {
        return quantity == null ? 1 : Math.max(1, quantity);
    }

    private boolean notEmpty(String s){ return s != null && !s.trim().isEmpty(); }
}
