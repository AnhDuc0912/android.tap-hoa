package com.example.hango.products;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("product_id")
    private int productId;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("price")
    private String price;

    @SerializedName("image_path")
    private String imagePath;

    @SerializedName("similarity")
    private float similarity;

    @SerializedName("category_id")
    private int categoryId;

    @SerializedName("label")
    private String label;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("unit")
    private String unit;

    @SerializedName("description")
    private String description;

    @SerializedName("image_source")
    private String imageSource;

    @SerializedName("notes")
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    // --- Getter methods (tuỳ bạn cần dùng đến trường nào thì thêm getter đó)
    public String getProductName() { return productName; }
    public String getImagePath() { return imagePath; }
    public String getCategoryName() { return categoryName; }
    public String getLabel() { return label; }
    public float getSimilarity() { return similarity; }
    public String getPrice() { return price; }
    // ... Thêm các getter khác nếu cần
}

