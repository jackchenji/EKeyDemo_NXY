package com.froad.ukey.http.bean;

import com.froad.ukey.http.interf.RequestParams;

import java.util.HashMap;
import java.util.Map;

public class AuthRequestParams implements RequestParams {

    private Map<String, String> params = new HashMap();

    public Map<String, String> getStringParams() {
        return this.params;
    }

    public void putParam(String key, String value) {
        if (value != null) {
            this.params.put(key, value);
        } else {
            this.params.remove(key);
        }

    }

    public void putParam(String key, boolean value) {
        if (value) {
            this.putParam(key, "true");
        } else {
            this.putParam(key, "false");
        }

    }

    public void putParam(String key, long value) {
        this.putParam(key, String.valueOf(value));
    }

    @Override
    public String toString() {
        return "AuthRequestParams{" +
                "params=" + params +
                '}';
    }
}
