package com.example.hango.api;

import com.google.gson.annotations.SerializedName;

public class ApiSingleResponse<T> {
    @SerializedName("query_id")   public Long queryId;
    @SerializedName("elapsed_ms") public Long elapsedMs;
    @SerializedName("result")     public T result;   // backend trả "result" thay vì "data" hay "results"
    @SerializedName("error")      public String error;
    public boolean isOk(){ return error == null || error.trim().isEmpty(); }
}
