package com.froad.ukey.simchannel.ref;

import com.froad.ukey.utils.np.TMKeyLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by FW on 2017/12/28.
 */

public class RefUtil {
    private static String TAG = "RefUtil";

    public static boolean classHassMethod(Class<?> cls, String name) {
        try {
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods)
                if (method.getName().equals(name))
                    return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static Method classGetMethod(Class<?> cls, String name) {
        try {
            Method[] methods = cls.getDeclaredMethods();
            //???????????
//            int paramsCount = 0;
//            Class[] ps = null;
//            Method resMethod = null;
            for (Method method : methods) {
                if (method.getName().equals(name)) {
                    return method;
//                    if (resMethod == null) {
//                        resMethod = method;
//                    }
//                    ps = method.getParameterTypes();
//                    if (ps == null) {
//                        TMKeyLog.d(TAG, "classGetMethod>>>method>>>" + method.getName() + ">>>cls:" + cls.getName() + ">>>ps is null");
//                    } else {
//                        TMKeyLog.d(TAG, "classGetMethod>>>method>>>" + method.getName() + ">>>cls:" + cls.getName() + ">>>ps:" + ps.length);
//                        int psLen = ps.length;
//                        for (int i = 0; i < psLen; i++) {
//                            TMKeyLog.d(TAG, "ps:" + ps.length + ">>>psType:" + ps[i].getName());
//                        }
//                        if (ps.length > paramsCount) {
//                            paramsCount = ps.length;
//                            resMethod = method;
//                        }
//                    }
                }
            }
//            return resMethod;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            TMKeyLog.d(TAG, "try " + name + " class error.");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean classSetVar(Class<?> c, String fieldName, Object instanct, Object object) {
        try {
            Field field = c.getDeclaredField(fieldName);
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            field.set(instanct, object);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Object classGetVar(Class<?> c, String fieldName, Object instanct) {
        try {
            Field field = c.getDeclaredField(fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(instanct);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getClassObject (Class<?> c) {
        try {
            Object obj = c.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
