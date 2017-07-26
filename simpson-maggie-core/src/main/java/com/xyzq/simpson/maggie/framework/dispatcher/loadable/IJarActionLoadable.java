package com.xyzq.simpson.maggie.framework.dispatcher.loadable;

import java.net.URLClassLoader;
import java.util.jar.JarFile;

/**
 * jar动作可加载接口
 */
public interface IJarActionLoadable {
    /**
     * 从Jar中加载动作
     *
     * @param jarFile Jar文件
     * @param classLoader 类加载器
     * @return 执行结果
     */
    boolean loadAction(JarFile jarFile, URLClassLoader classLoader);
}
