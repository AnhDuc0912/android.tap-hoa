package com.example.hango.api;

public class SearchTextRequest {
    public String q;
    public Integer k;

    public SearchTextRequest(String q, Integer k) {
        this.q = q;
        this.k = k;
    }
}
