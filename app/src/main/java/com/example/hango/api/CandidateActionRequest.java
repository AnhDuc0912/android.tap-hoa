package com.example.hango.api;

public class CandidateActionRequest {
    public long query_id;        // bắt buộc
    public long sku_id;          // bắt buộc
    public String action;        // "click" | "purchase" | "dwell"
    public Double dwell_time;    // giây; có thể null

    public CandidateActionRequest(long queryId, long skuId, String action, Double dwellSeconds) {
        this.query_id = queryId;
        this.sku_id = skuId;
        this.action = action;
        this.dwell_time = dwellSeconds;
    }
}
