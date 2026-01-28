package com.example.hango.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiListResponse<T> {
    @SerializedName("query_id")         public Long queryId;
    @SerializedName("query_text")       public String queryText;      // có ở text_with_image & similar-skus
    @SerializedName("elapsed_ms")       public Long elapsedMs;
    @SerializedName("total")            public Integer total;
    @SerializedName("results")          public List<T> results;
    @SerializedName("applied_re_rank")  public Boolean appliedReRank; // có ở /search/image
    @SerializedName("applied_reason")   public String appliedReason;  // có ở /search/image
    @SerializedName("error")            public String error;

    public boolean isOk(){
        return error == null || error.trim().isEmpty();
    }
}
