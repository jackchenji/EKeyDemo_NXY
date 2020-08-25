package com.froad.ukey.http.exception;

public class AuthError extends Exception {

    protected String code;
    protected String errorMessage;
    protected Throwable cause;

    public AuthError(String errorCode, String message, Throwable cause) {
        super(genMessage(errorCode, message), cause);
        this.cause = cause;
        this.code = errorCode;
    }

    public AuthError(String errorCode, String message) {
        super(genMessage(errorCode, message));
        this.code = errorCode;
        this.errorMessage = message;
    }

    public AuthError(String message) {
        super(message);
    }

    public AuthError() {
    }

    private static String genMessage(String code, String message) {
        return "[" + code + "] " + message;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getCode() {
        return this.code;
    }

    public interface ErrorCode {
        String SERVICE_NET_ERROR = "283504";
        String SERVICE_DATA_ERROR = "283505";
    }
}
