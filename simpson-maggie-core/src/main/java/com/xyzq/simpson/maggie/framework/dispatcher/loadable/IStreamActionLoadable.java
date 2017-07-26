package com.xyzq.simpson.maggie.framework.dispatcher.loadable;

import java.io.IOException;
import java.io.InputStream;

/**
 * 流动作可加载接口
 */
public interface IStreamActionLoadable {
    /**
     * 加载脚本动作输入流
     *
     * @param path 路径
     * @param stream 动作文件输入流
     * @param encoding 编码
     * @param classLoader 类加载器
     * @return 执行结果
     */
    boolean loadAction(String path, InputStream stream, int encoding, ClassLoader classLoader) throws IOException;
}
