package com.xyzq.simpson.maggie.framework.dispatcher;

import com.xyzq.simpson.base.type.Array;
import com.xyzq.simpson.base.type.core.ICollection;
import com.xyzq.simpson.maggie.framework.dispatcher.core.IActionDispatcher;

/**
 * 动作
 */
public abstract class ActionDispatcher implements IActionDispatcher {
    /**
     * 支持的后缀数组
     */
    protected Array<String> suffixes = null;


    /**
     * 获取支持的后缀集合
     *
     * @return 后缀集合
     */
    @Override
    public ICollection<String> suffixes() {
        return suffixes;
    }
}
