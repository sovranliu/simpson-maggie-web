package com.xyzq.simpson.maggie.framework.dispatcher.loadable;

import com.xyzq.simpson.maggie.framework.action.core.IAction;

/**
 * 类动作可加载接口
 */
public interface IClassActionLoadable {
    /**
     * 加载JAVA动作类
     *
     * @param path 路径
     * @param clazz 动作类
     * @return 执行结果
     */
    boolean loadAction(String path, Class<IAction> clazz);
}
