package com.xyzq.simpson.maggie.framework.dispatcher;

import com.xyzq.simpson.base.type.Array;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IFileActionLoadable;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IStreamActionLoadable;
import com.xyzq.simpson.maggie.utility.template.MaggieResourceCache;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

/**
 * 模板动作分发器
 */
public class TemplateActionDispatcher extends ActionDispatcher implements IStreamActionLoadable, IFileActionLoadable {
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(TemplateActionDispatcher.class);
    /**
     * 模版文件根目录
     */
    public File root = null;
    /**
     * 模版引擎
     */
    protected VelocityEngine engine = null;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "maggie");
        properties.setProperty("maggie.resource.loader.description", "Maggie Template");
        properties.setProperty("maggie.resource.loader.class", "com.xyzq.simpson.maggie.utility.template.MaggieTemplateLoader");
        properties.setProperty("maggie.resource.loader.path", root.getAbsolutePath());
        properties.setProperty("resource.manager.cache.class", "com.xyzq.simpson.maggie.utility.template.MaggieResourceCache");
        properties.setProperty("maggie.resource.loader.cache", "true");
        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");
        engine = new VelocityEngine();
        engine.init(properties);
        this.suffixes = new Array<String>(new String[] {"json", "html", "txt", "xml", "code", "css", "js", "url"});
        return true;
    }

    /**
     * 终止
     */
    @Override
    public void terminate() {
        engine = null;
    }

    /**
     * 动作执行
     *
     * @param path    路径
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    @Override
    public String execute(String path, Visitor visitor, Context context) throws Exception {
        if ("/".equals(File.separator)) {
            path = path.replace("\\", "/");
        }
        else {
            path = path.replace("\\", "/");
        }
        if (("/" + path).startsWith(context.uri() + ".")) {
            prepareContext(context);
        }
        render(path, visitor, context);
        return null;
    }

    /**
     * 准备上下文
     *
     * @param context 上下文
     */
    protected void prepareContext(Context context) {
        for (ILink<String, Object[]> link : context.parameters()) {
            if ((null != link.destination()) && (0 != (link.destination()).length)) {
                if (1 == (link.destination()).length) {
                    context.set(link.origin(), (link.destination())[0]);
                }
                else {
                    context.set(link.origin(), link.destination());
                }
            }
        }
    }

    /**
     * 渲染
     *
     * @param visitor 访问者
     * @param context 上下文
     * @param path 模版路径
     */
    public void render(String path, Visitor visitor, Context context) throws Exception {
        visitor.addHeader("Access-Control-Allow-Origin", "*");
        if (path.endsWith(".json")) {
            visitor.response().setContentType("application/json");
        }
        else if (path.endsWith(".html")) {
            visitor.response().setContentType("text/html");
        }
        else if (path.endsWith(".txt")) {
            visitor.response().setContentType("text/plain");
        }
        if (path.endsWith(".xml")) {
            visitor.response().setContentType("application/xml");
        }
        else if (path.endsWith(".code")) {
            int code = Integer.valueOf(path.replace(".code", "")).intValue();
            if(code > 300 && code < 400) {
                visitor.response().setStatus(code);
                visitor.response().setHeader("location", (String) context.get("location"));
            }
            else if(code > 400) {
                visitor.response().sendError(Integer.valueOf(path.replace(".code", "")).intValue());
            }
            else {
                visitor.response().setStatus(code);
            }
            return;
        }
        else if (path.endsWith(".css")) {
            visitor.response().setContentType("text/css");
        }
        else if (path.endsWith(".js")) {
            visitor.response().setContentType("application/javascript");
        }
        Template template = engine.getTemplate(path);
        if (null == template) {
            logger.error("template '" + path + "' not exist");
            return;
        }
        if (path.endsWith(".url")) {
            StringWriter writer = new StringWriter();
            template.merge(context.template(), writer);
            writer.close();
            visitor.response().sendRedirect(writer.toString());
        }
        else {
            template.merge(context.template(), visitor.response().getWriter());
        }
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
        if ("/".equals(File.separator)) {
            path = path.replace("\\", File.separator);
        }
        else {
            path = path.replace("/", File.separator);
        }
        if(file.exists() && file.isDirectory()) {
            for(File item : file.listFiles()) {
                if(!loadAction(path + File.separator + item.getName(), item)) {
                    return false;
                }
            }
            return true;
        }
        MaggieResourceCache.instance().delete(path);
        return true;
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
        if ("/".equals(File.separator)) {
            path = path.replace("\\", File.separator);
        }
        else {
            path = path.replace("/", File.separator);
        }
        MaggieResourceCache.instance().delete(path);
        return true;
    }
}
