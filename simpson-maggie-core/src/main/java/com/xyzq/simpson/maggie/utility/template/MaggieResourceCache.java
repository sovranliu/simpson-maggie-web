package com.xyzq.simpson.maggie.utility.template;

import org.apache.velocity.runtime.resource.ResourceCacheImpl;
import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * 资源缓存
 */
public class MaggieResourceCache extends ResourceCacheImpl {
    /**
     * 单件实例
     */
    private static MaggieResourceCache instance = null;


    /**
     * 构造函数
     */
    public MaggieResourceCache() {
        instance = this;
    }

    /**
     * 获取单件对象
     *
     * @return 单件对象
     */
    public static MaggieResourceCache instance() {
        return instance;
    }

    /**
     * 删除资源缓存
     *
     * @param path 资源路径
     */
    public void delete(String path) {
        super.remove(String.valueOf(ResourceManager.RESOURCE_TEMPLATE) + path);
    }
}
