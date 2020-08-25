package com.froad.ukey.http.interf;

import com.froad.ukey.http.exception.AuthError;

public interface Parser<T> {
    T parse(String jStr) throws AuthError;
}
