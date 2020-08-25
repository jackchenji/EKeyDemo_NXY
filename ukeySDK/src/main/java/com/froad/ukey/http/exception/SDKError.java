package com.froad.ukey.http.exception;

public class SDKError extends AuthError {

    public SDKError(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.cause = cause;
        this.code = errorCode;
    }

    public SDKError(String errorCode, String message) {
        super(errorCode, message);
        this.code = errorCode;
        this.errorMessage = message;
    }

    public SDKError(String message) {
        super(message);
    }

    public SDKError() {
    }

    public interface ErrorCode {
        int NETWORK_REQUEST_ERROR = 283504;
        int ACCESS_TOKEN_DATA_ERROR = 283505;
        int LOAD_JNI_LIBRARY_ERROR = 283506;
    }
}
