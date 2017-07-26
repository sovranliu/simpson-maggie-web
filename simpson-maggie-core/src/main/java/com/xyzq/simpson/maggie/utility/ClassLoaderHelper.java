package com.xyzq.simpson.maggie.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 类加载器帮助类
 *
 * 支持加载多个Jar包
 */
public class ClassLoaderHelper {
    /**
     * 隐藏构造函数
     */
    private ClassLoaderHelper() { }

    /**
     * 添加URL
     *
     * @param classLoader 类加载器
     * @param url URL
     */
    public static void addURL(URLClassLoader classLoader, URL url) {
        Method method = null;
        try {
            method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(classLoader, url);
        }
        catch (NoSuchMethodException e) { }
        catch (InvocationTargetException e) { }
        catch (IllegalAccessException e) { }
    }
}
