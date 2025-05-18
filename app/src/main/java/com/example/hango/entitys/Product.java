package com.example.hango.entitys;

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
    public float getSimilarity() { return similarity; }
    public String getPrice() { return price; }

    // ... Thêm các getter khác nếu cần

    // Setter để gán dữ liệu khi tạo sản phẩm mới
    public void setProductName(String productName) { this.productName = productName; }
    public void setPrice(String price) { this.price = price; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setDescription(String description) { this.description = description; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setSimilarity(float similarity) { this.similarity = similarity; }
    public void setProductId(int productId) { this.productId = productId; }
}

