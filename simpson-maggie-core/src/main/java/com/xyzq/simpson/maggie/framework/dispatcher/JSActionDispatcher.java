package com.xyzq.simpson.maggie.framework.dispatcher;

import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.Array;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.safe.Table;
import com.xyzq.simpson.maggie.framework.action.Action;
import com.xyzq.simpson.maggie.framework.action.JavaScriptAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IFileActionLoadable;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IStreamActionLoadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * JS动作分发器
 */
public class JSActionDispatcher extends ActionDispatcher implements IStreamActionLoadable, IFileActionLoadable {
    /**
     * JS动作后缀
     */
    public final static String JSACTION_SUFFIX = "javascript";
    /**
     * JS动作函数库后缀
     */
    public final static String FUNCTION_SUFFIX = "function";
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(JSActionDispatcher.class);
    /**
     * 文件根目录
     */
    public File root = null;
    /**
     * 动作映射，路径与动作对象
     */
    protected Table<String, JavaScriptAction> actions = null;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        this.suffixes = new Array<String>(3);
        suffixes.set(0, ACTION_SUFFIX);
        suffixes.set(1, JSACTION_SUFFIX);
        suffixes.set(2, FUNCTION_SUFFIX);
        actions = new Table<String, JavaScriptAction>();
        return true;
    }

    /**
     * 终止
     */
    @Override
    public void terminate() {
        suffixes = null;
        for(ILink<String, JavaScriptAction> link : actions) {
            link.destination().terminate();
        }
        actions = null;
    }

    /**
     * 动作执行
     *
     * @param path 路径
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    @Override
    public String execute(String path, Visitor visitor, Context context) throws Exception {
        JavaScriptAction action = actions.get(path);
        if(null == action) {
            return null;
        }
        return action.execute(visitor, context);
    }

    /**
     * 加载脚本动作文件或者目录
     *
     * @param path 路径
     * @param file 文件或者目录
     * @return 执行结果
     */
    @Override
    public boolean loadAction(String path, File file) throws IOException {
        path = path.replace("\\", "/");
        int i = path.lastIndexOf(".");
        if(-1 == i) {
            return false;
        }
        String suffix = path.substring(i + 1);
        if (!file.exists()) {
            Action action = actions.delete(path);
            if(null != action) {
                action.terminate();
            }
            return true;
        }
        if (ACTION_SUFFIX.equalsIgnoreCase(suffix) || JSACTION_SUFFIX.equalsIgnoreCase(suffix)) {
            JavaScriptAction action = new JavaScriptAction();
            action.file = file;
            if (!action.initialize()) {
                throw new RuntimeException("action initialize failed, file = " + file.getAbsolutePath());
            }
            actions.put(path, action);
            return true;
        }
        else if (FUNCTION_SUFFIX.equalsIgnoreCase(suffix)) {
            try {
                JavaScriptAction.engine().eval(Text.loadFile(file, Encoding.ENCODING_UTF8));
            }
            catch (Exception e) {
                throw new RuntimeException("maggie load js function failed", e);
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 加载脚本动作输入流
     *
     * @param path 路径
     * @param stream 动作文件输入流
     * @param encoding 编码
     * @param classLoader 类加载器
     * @return 执行结果
     */
    @Override
    public boolean loadAction(String path, InputStream stream, int encoding, ClassLoader classLoader) throws IOException {
        path = path.replace("\\", "/");
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        int i = path.lastIndexOf(".");
        if (i > 0) {
            String suffix = path.substring(i + 1);
            if (null == stream) {
                actions.delete(path);
                return true;
            }
            if (ACTION_SUFFIX.equalsIgnoreCase(suffix) || JSACTION_SUFFIX.equalsIgnoreCase(suffix)) {
                JavaScriptAction action = new JavaScriptAction();
                action.content = Text.loadStream(stream, encoding);
                if(JSActionDispatcher.class.getClassLoader() != classLoader) {
                    action.classLoader = classLoader;
                }
                stream.close();
                if (!action.initialize()) {
                    throw new RuntimeException("javascript action initialize failed, path = " + path);
                }
                actions.put(path, action);
            }
            else if (FUNCTION_SUFFIX.equalsIgnoreCase(suffix)) {
                try {
                    String text = Text.loadStream(stream, encoding);
                    stream.close();
                    JavaScriptAction.engine().eval(text);
                }
                catch (Exception e) {
                    throw new RuntimeException("javascript function initialize failed, path = " + path, e);
                }
            }
        }
        return true;
    }
}
