package com.xyzq.simpson.maggie.logic;

import com.xyzq.simpson.base.model.core.IModule;
import com.xyzq.simpson.base.runtime.Invoke;
import com.xyzq.simpson.lisa.Client;
import com.xyzq.simpson.maggie.access.spring.MaggieSpringSupport;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;

/**
 * 外部调用服务
 */
public class InvokeService implements IModule {
    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        return true;
    }

    /**
     * 终止
     */
    @Override
    public void terminate() { }

    /**
     * 获取Spring应用上下文
     *
     * @return Spring应用上下文
     */
    public ApplicationContext applicationContext() {
        return MaggieSpringSupport.applicationContext();
    }

    /**
     * 获取Spring应用上下文
     *
     * @param name Bean名称
     * @return Spring应用上下文
     */
    public Object getBean(String name) {
        ApplicationContext applicationContext = applicationContext();
        if(null == applicationContext) {
            return null;
        }
        return applicationContext.getBean(name);
    }

    /**
     * 调用指定名称的服务
     *
     * @param serviceName 服务名
     * @param invoke 调用
     * @return 执行结果
     */
    public Object invoke(String serviceName, Invoke invoke) throws Exception {
        ApplicationContext applicationContext = applicationContext();
        if((null != applicationContext) && (applicationContext.containsBean(serviceName))) {
            Object bean = applicationContext.getBean(serviceName);
            if (null != bean) {
                return invoke.invoke(bean);
            }
        }
        Client.instance().loadService(serviceName);
        return Client.instance().invoke(serviceName, invoke.method, invoke.parameters);
    }
}
