package com.xyzq.simpson.maggie.access.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 获取Spring上下文对象
 */
public class MaggieSpringSupport implements ApplicationContextAware {
    /**
     * Spring应用上下文
     */
    private static ApplicationContext applicationContext = null;


    /**
     * 获取Spring应用上下文
     *
     * @return Spring应用上下文
     */
    public static ApplicationContext applicationContext() {
        return MaggieSpringSupport.applicationContext;
    }

    /**
     * 设置Spring应用上下文
     *
     * @param applicationContext Spring应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MaggieSpringSupport.applicationContext = applicationContext;
    }
}
