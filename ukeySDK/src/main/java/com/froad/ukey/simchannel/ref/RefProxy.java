package com.froad.ukey.simchannel.ref;

/**
 * Created by FW on 2017/12/28.
 */
import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public class RefProxy
        implements InvocationHandler
{
    private final static String TAG = "RefProxy";
    protected Object mIns = null;
    protected Class<?> mCls = null;//原始类
    protected Class<?> mImpCls = null;//动态加载类

    protected Object init(Class<?> impCls, Class<?> cls, Object ins) {
        Object ret = null;
        this.mIns = ins;
        this.mImpCls = impCls;
        this.mCls = cls;
        ret = Proxy.newProxyInstance(this.mImpCls.getClassLoader(), new Class[] { this.mImpCls }, this);
        return ret;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Method localMethod = null;
        try {
            String methodName = method.getName();
            TMKeyLog.d(TAG, "methodName:" + methodName);
            localMethod = this.mCls.getDeclaredMethod(methodName, method.getParameterTypes());
        } catch (Exception e) {
            try {
                String methodName = method.getName();
                TMKeyLog.d(TAG, "methodName:" + methodName);
                localMethod = this.mCls.getMethod(methodName, method.getParameterTypes());
            } catch (Exception e2) {
                throw new Exception("proxy error." + e.toString());
            }
        }
        if (localMethod == null) {
            throw new Exception("proxy error.");
        }
        localMethod.setAccessible(true);
        if (Modifier.isStatic(method.getModifiers())) {
            return localMethod.invoke(null, args);
        }
        return localMethod.invoke(this.mIns, args);
    }
}
