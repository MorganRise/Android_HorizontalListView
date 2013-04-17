package jp.co.tdkn.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * リフレクションを使ったアクセスを提供するクラス
 * 
 * @note 一度取り出したメソッド／フィールド情報はキャッシュします
 * @note スレッドセーフではありません
 */
public class ReflectionProxy {

    private final HashMap<String, Field> mFieldCache = new HashMap<String, Field>();
    private final HashMap<String, Method> mMethodCache = new HashMap<String, Method>();

    protected final <T> int getFieldValueAsInt(Class<T> cl, String fieldName,
            T instance) {
        try {
            Field f = getField(cl, fieldName);
            return f.getInt(instance);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(fieldName + " doesn't exists.");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(fieldName + " cannot access.");
        }
    }

    protected final <T> boolean getFieldValueAsBoolean(Class<T> cl,
            String fieldName, T instance) {
        try {
            Field f = getField(cl, fieldName);
            return f.getBoolean(instance);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(fieldName + " doesn't exists.");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(fieldName + " cannot access.");
        }
    }

    protected final <T> void setFieldValue(Class<T> cl, String fieldName,
            int value, T instance) {
        try {
            Field f = getField(cl, fieldName);
            f.setInt(instance, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(fieldName + " doesn't exists.");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(fieldName + " cannot access.");
        }
    }

    protected final <T> void setFieldValue(Class<T> cl, String fieldName,
            boolean value, T instance) {
        try {
            Field f = getField(cl, fieldName);
            f.setBoolean(instance, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(fieldName + " doesn't exists.");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(fieldName + " cannot access.");
        }
    }

    protected final Field getField(Class<?> cl, String fieldName)
            throws NoSuchFieldException {
        Field f = mFieldCache.get(fieldName);
        if (f == null) {
            f = cl.getDeclaredField(fieldName);
            f.setAccessible(true);
        }
        mFieldCache.put(fieldName, f);

        return f;
    }

    protected final Method getMethod(Class<?> cl, String methodName,
            Class<?>... paramTypes) throws NoSuchMethodException {
        final String key = (paramTypes == null || paramTypes.length == 0) ? methodName
                : methodName + paramTypes.toString();
        Method m = mMethodCache.get(key);
        if (m == null) {
            m = cl.getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
        }
        mMethodCache.put(key, m);

        return m;
    }

}
