package com.xyzq.simpson.maggie.framework.dispatcher.loadable;

import java.io.File;
import java.io.IOException;

/**
 * 文件动作可加载接口
 */
public interface IFileActionLoadable {
    /**
     * 加载脚本动作文件或者目录
     *
     * @param path 路径
     * @param file 文件或者目录
     * @return 执行结果
     */
    boolean loadAction(String path, File file) throws IOException;
}
