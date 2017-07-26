package com.xyzq.simpson.maggie.framework.action;

import com.xyzq.simpson.base.text.listen.StringListenable;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.safe.Table;
import com.xyzq.simpson.maggie.Maggie;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.etc.Serial;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.List;
import com.xyzq.simpson.maggie.framework.Logger;
import com.xyzq.simpson.maggie.framework.Visitor;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * JavaScript脚本动作
 */
public class JavaScriptAction extends Action {
    /**
     * 内置服务接口对象变量名
     */
    public final static String SERVICE_VARIABLE_NAME = "g_maggieService";


    /**
     * 脚本引擎对象
     */
    private static ScriptEngine engine = null;
    /**
     * 编译后的脚本
     */
    public Invocable script = null;
    /**
     * 脚本文件
     */
    public File file = null;
    /**
     * 脚本内容
     */
    public String content = null;
    /**
     * 脚本函数名称
     */
    public String function = null;
    /**
     * 预定义
     */
    public Table<String, String> defines = null;
    /**
     * 类加载器
     */
    public ClassLoader classLoader = null;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    public boolean initialize() {
        if(null == engine) {
            if(null == engine()) {
                logger.error("script engine initialize failed");
                return false;
            }
        }
        try {
            // 生成脚本运行时
            engine.eval(initContent());
            script = (Invocable) engine;
        }
        catch(Exception ex) {
            if(null != content) {
                logger.error("script load or compile failed, content = \n" + content, ex);
            }
            else if(null != file) {
                logger.error("script load or compile failed, file = " + file.getAbsolutePath(), ex);
            }
            return false;
        }
        return true;
    }

    /**
     * 终止
     */
    public void terminate() {
        script = null;
        engine = null;
    }

    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        if(null != defines) {
            for(ILink<String, String> link : defines) {
                context.put(link.origin(), link.destination());
            }
        }
        String result = super.execute(visitor, context);
        if(null == result) {
            return invoke(script, classLoader, function, visitor, context);
        }
        return result;
    }

    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作
     */
    public static String invoke(Invocable invocable, ClassLoader classLoader, String function, Visitor visitor, Context context) throws Exception {
        try {
            if(null != classLoader && JavaScriptAction.class.getClassLoader() != classLoader) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return (String) invocable.invokeFunction(function, visitor, context);
                }
                finally {
                    Thread.currentThread().setContextClassLoader(loader);
                }
            }
            else {
                return (String) invocable.invokeFunction(function, visitor, context);
            }
        }
        finally {
            Maggie.bridgeService().declare(null, null);
        }
    }

    /**
     * 初始脚本内容
     *
     * @return 脚本内容
     */
    public String initContent() throws Exception {
        StringBuilder builder = new StringBuilder();
        this.defines = null;
        StringListenable listenable = new StringListenable(builder) {
            /**
             * 监听回调
             *
             * @param line 一行
             */
            @Override
            public void onRead(String line) {
                if(Text.isBlank(line) || line.trim().startsWith("//")) {
                    super.onRead(line);
                }
                else if(line.startsWith("#define")) {
                    if(null == JavaScriptAction.this.defines) {
                        JavaScriptAction.this.defines = new Table<String, String>();
                    }
                    line = line.substring("#define".length()).trim();
                    int i = line.indexOf(" ");
                    if(i > 0) {
                        defines.put(line.substring(0, i), line.substring(i + 1).trim());
                    }
                }
                else if(line.startsWith("#declare")) {
                    line = line.substring("#declare".length()).trim();
                    String serviceName = Text.substring(line, "$('", "')");
                    super.onRead("$().declare('" + serviceName + "', '" + Text.substring(line, ").", null) + "');");
                }
                else if(line.startsWith("#include")) {
                    String fileName = Text.substring(line, "<", ">");
                    if (null == includes) {
                        includes = new List<String>();
                    }
                    includes.add(fileName);
                }
                else {
                    super.onRead(line);
                }
            }
        };
        if(null != content) {
            Text.loadText(content, listenable);
        }
        else if(null != file) {
            Text.loadFile(file, Encoding.ENCODING_UTF8, listenable);
        }
        // 支持服务选择器
        int i = 0;
        while(true) {
            int j = builder.indexOf("$('", i);
            if(-1 == j) {
                break;
            }
            j = builder.indexOf("').", j);
            if(-1 == j) {
                break;
            }
            j += "').".length();
            int k = builder.indexOf("(", j);
            if(-1 == k) {
                break;
            }
            String methodString = builder.substring(j, k);
            i = k + 1;
            while(true) {
                if(builder.substring(i, i + 1).equals(")")) {
                    i = -1;
                    break;
                }
                else if(!Text.isBlank(builder.substring(i, i + 1))) {
                    break;
                }
                i++;
            }
            if(-1 == i) {
                builder.replace(k, k + 1, "'");
            }
            else {
                builder.replace(k, k + 1, "',");
            }
            builder.insert(j, "invoke('");
            i = j;
        }
        this.function = "f" + Serial.makeLocalID();
        return "function " + function + "(visitor, context) {\n" + builder.toString() + "\n}";
    }

    /**
     * 获取脚本引擎
     *
     * @return 脚本引擎对象
     */
    public static ScriptEngine engine() {
        if(null == engine) {
            synchronized (JavaScriptAction.class) {
                if(null == logger) {
                    logger = LoggerFactory.getLogger(JavaScriptAction.class);
                }
                if(null == engine) {
                    engine = new ScriptEngineManager().getEngineByName("javascript");
                    engine.put(SERVICE_VARIABLE_NAME, Maggie.bridgeService());
                    engine.put("logger", new Logger());
                    try {
                        engine.eval(loadBridgeText());
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return engine;
    }

    /**
     * 加载桥接脚本代码
     *
     * @return 桥接脚本代码
     */
    private static String loadBridgeText() throws Exception {
        InputStream inputStream = JavaScriptAction.class.getResourceAsStream("/bridge.js");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while((line = bufferedReader.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
        }
        return builder.toString();
    }
}
