package com.xyzq.simpson.maggie.framework.dispatcher;

import com.xyzq.simpson.base.runtime.Kind;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.Array;
import com.xyzq.simpson.base.type.List;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.core.ITable;
import com.xyzq.simpson.base.type.safe.Table;
import com.xyzq.simpson.maggie.access.spring.MaggieAction;
import com.xyzq.simpson.maggie.framework.action.JavaAction;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IClassActionLoadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java动作分发器
 */
public class JavaActionDispatcher extends ActionDispatcher implements IClassActionLoadable {
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(JavaActionDispatcher.class);
    /**
     * 缓存的类名与Java动作映射
     */
    protected ITable<String, JavaAction> cached = null;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        this.suffixes = new Array<String>(1);
        suffixes.set(0, JavaAction.ACTION_SUFFIX);
        cached = new Table<String, JavaAction>();
        return true;
    }

    /**
     * 终止
     */
    @Override
    public void terminate() {
        suffixes = null;
        if(null != cached) {
            for(ILink<String, JavaAction> link : cached) {
                link.destination().terminate();
            }
            cached = null;
        }
    }

    /**
     * 动作执行
     *
     * @param path Java类名
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    @Override
    public String execute(String path, Visitor visitor, Context context) throws Exception {
        // 去除.java的suffix
        String className = path.substring(0, path.length() - JavaAction.ACTION_SUFFIX.length() - 1);
        JavaAction action = cached.get(path);
        if(null == action) {
            synchronized (this) {
                action = cached.get(path);
                if(null == action) {
                    try {
                        if(loadAction((Class<IAction>) Kind.classForName(className))) {
                            action = cached.get(path);
                        }
                        else {
                            logger.error("java action initialize failed, class = " + className);
                        }
                    }
                    catch (Throwable throwable) {
                        logger.error("build java action failed, class = " + path, throwable);
                    }
                }
            }
        }
        if(null == action) {
            throw new RuntimeException("java action not found, path = " + path);
        }
        return action.execute(visitor, context);
    }

    /**
     * 加载JAVA动作类
     *
     * @param clazz 动作类
     * @return 执行结果
     */
    public boolean loadAction(Class<IAction> clazz) {
        return loadAction(clazz.getName() + "." + JavaAction.ACTION_SUFFIX, clazz);
    }

    /**
     * 加载JAVA动作类
     *
     * @param path 路径
     * @param clazz 动作类
     * @return 执行结果
     */
    @Override
    public boolean loadAction(String path, Class<IAction> clazz) {
        if(null != cached.get(path)) {
            logger.warn("class action load too many times");
            return true;
        }
        JavaAction javaAction = new JavaAction();
        javaAction.clazz = clazz;
        if(javaAction.initialize()) {
            cached.put(path, javaAction);
            return true;
        }
        else {
            logger.warn("class action initialize failed, uri = " + path);
            return false;
        }
    }
}
