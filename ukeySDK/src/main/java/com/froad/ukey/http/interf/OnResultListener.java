package com.froad.ukey.http.interf;

import com.froad.ukey.http.exception.AuthError;

public interface OnResultListener<T> {
    void onResult(T obj);

    void onError(AuthError authError);
}
