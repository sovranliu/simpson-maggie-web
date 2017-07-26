package com.xyzq.simpson.maggie.framework.action.core;

import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;

/**
 * 动作处理器接口
 */
public interface IAction {
    /**
     * 动作执行
     *
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    String execute(Visitor visitor, Context context) throws Exception;
}
