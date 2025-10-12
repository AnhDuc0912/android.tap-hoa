// SearchImageItem.java
package com.example.hango.api;

import com.google.gson.annotations.SerializedName;

public class SearchImageItem {
    @SerializedName("sku_id")     public Long skuId;
    @SerializedName("image_path") public String imagePath;
    @SerializedName("score")      public Double score;
    @SerializedName("sku_name")   public String skuName;
    @SerializedName("brand_name") public String brandName;
    @SerializedName("ocr_text")   public String ocrText;
}
