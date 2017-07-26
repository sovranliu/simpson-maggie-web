package com.xyzq.simpson.maggie.framework.action;

import com.xyzq.simpson.base.type.core.IList;
import com.xyzq.simpson.maggie.Maggie;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.base.model.core.IModule;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动作
 */
public abstract class Action implements IAction, IModule {
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(Action.class);
    /**
     * 前置动作路径列表
     */
    public IList<String> includes = null;


    /**
     * 动作执行
     *
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        if(null == includes) {
            return null;
        }
        String result = null;
        for(String include : includes) {
            result = Maggie.dispatcher().execute(include, visitor, context);
            if(null != result) {
                break;
            }
        }
        return result;
    }
}
