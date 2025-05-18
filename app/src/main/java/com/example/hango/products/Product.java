package com.example.hango.products;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("product_name")
    private String name;

    @SerializedName("price")
    private String price;

    @SerializedName("image_path")
    private String image;

    @SerializedName("similarity")
    private double similarity;

    public String getName() { return name; }

    public String getPrice() { return price; }
    public String getImagePath() { return image;}
    public double getSimilarity() { return similarity; }
}
