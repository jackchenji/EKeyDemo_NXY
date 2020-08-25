package com.froad.ukey.http.bean;

import com.froad.ukey.http.exception.AuthError;
import com.froad.ukey.http.interf.Parser;
import com.froad.ukey.utils.np.TMKeyLog;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthResultParser implements Parser<AuthResponseResult> {

    private final static String TAG = "AuthResultParser";
    public final static String HttpCRSuccess = "0000";
    public final static String HttpCRResCode = "resultCode";
    public final static String HttpCRResMessage = "resultDesc";
    public final static String HttpCRData = "data";
    public static String HttpSuccess = HttpCRSuccess;
    public static String HttpResCode = HttpCRResCode;
    public static String HttpResMessage = HttpCRResMessage;

    public AuthResponseResult parse(String json) throws AuthError {
        TMKeyLog.d(TAG, "parse>>>json:" + json);
        try {
            JSONObject jsonObject = new JSONObject(json);
            String httpResCode = jsonObject.optString(HttpResCode);
            String httpResMessage = jsonObject.optString(HttpResMessage);
            if (HttpSuccess.equalsIgnoreCase(httpResCode)) { //请求成功
                AuthResponseResult result = new AuthResponseResult();
                result.setResultCode(httpResCode);
                result.setResultDesc(httpResMessage);
                result.setData(jsonObject.optString(HttpCRData));
                return result;
            } else {
                AuthError error = new AuthError(jsonObject.optString(HttpResCode), jsonObject.optString(HttpResMessage));
                throw error;
            }
        } catch (JSONException e) {
            throw new AuthError(AuthError.ErrorCode.SERVICE_DATA_ERROR, "Server illegal response " + json, e);
        }
    }
}
