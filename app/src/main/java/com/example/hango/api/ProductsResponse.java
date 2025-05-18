package com.example.hango.api;

import com.example.hango.products.Product;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProductsResponse {
    @SerializedName("products")
    private List<Product> products;
    public List<Product> getProducts() { return products; }
}

