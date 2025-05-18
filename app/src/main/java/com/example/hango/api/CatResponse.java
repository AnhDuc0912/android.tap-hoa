package com.example.hango.api;

import com.example.hango.products.Category;
import com.example.hango.products.Product;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CatResponse {
    @SerializedName("categories")
    private List<Category> categories;
    public List<Category> getCategories() { return categories; }
}
