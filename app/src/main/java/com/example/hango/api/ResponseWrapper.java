package com.example.hango.api;

import com.example.hango.products.Product;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseWrapper {
    @SerializedName("predicted_category")
    private String predictedCategory;

    @SerializedName("similar_products")
    private List<Product> similarProducts;

    @SerializedName("total_results")
    private int totalResults;

    public String getPredictedCategory() { return predictedCategory; }
    public List<Product> getSimilarProducts() { return similarProducts; }
    public int getTotalResults() { return totalResults; }
}

