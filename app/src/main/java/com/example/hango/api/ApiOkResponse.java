package com.example.hango.api;

public class ApiOkResponse {
    public Boolean ok;
    public String error;

    public boolean isOk() {
        return ok != null && ok;
    }
}
