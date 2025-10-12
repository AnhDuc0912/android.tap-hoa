// SearchImageResponse.java
package com.example.hango.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchImageResponse {
    @SerializedName("query_id") public Long queryId;
    @SerializedName("filename") public String filename;
    @SerializedName("total")    public Integer total;
    @SerializedName("results")  public List<SearchImageItem> results;
    @SerializedName("error")    public String error; // nếu có lỗi từ server
}
