package com.xyzq.simpson.maggie.framework.dispatcher.core;

import com.xyzq.simpson.base.model.core.IModule;
import com.xyzq.simpson.base.type.core.ICollection;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;

/**
 * 动作分发器
 */
public interface IActionDispatcher extends IModule {
    /**
     * 路由动作名称
     */
    public final static String ACTION_ROUTE = "route";
    /**
     * 动作后缀
     */
    public final static String ACTION_SUFFIX = "action";


    /**
     * 获取支持的后缀集合
     *
     * @return 后缀集合
     */
    ICollection<String> suffixes();

    /**
     * 动作执行
     *
     * @param path 路径
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    String execute(String path, Visitor visitor, Context context) throws Exception;
}
