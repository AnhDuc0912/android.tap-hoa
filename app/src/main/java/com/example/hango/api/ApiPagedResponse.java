package com.example.hango.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiPagedResponse<T> {
    @SerializedName("query_id")   public Long queryId;
    @SerializedName("query_text") public String queryText;
    @SerializedName("paging")     public Paging paging;
    @SerializedName("results")    public List<T> results;
    @SerializedName("error")      public String error;

    public boolean isOk(){
        return error == null || error.trim().isEmpty();
    }

    public static class Paging {
        @SerializedName("page")        public int page;
        @SerializedName("page_size")   public int pageSize;
        @SerializedName("total")       public int total;
        @SerializedName("total_pages") public int totalPages;
        @SerializedName("has_next")    public boolean hasNext;
        @SerializedName("sql_offset")  public int sqlOffset;
        @SerializedName("sql_limit")   public int sqlLimit;
    }
}
