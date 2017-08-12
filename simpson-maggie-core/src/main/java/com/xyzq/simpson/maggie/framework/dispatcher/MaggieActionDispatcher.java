package com.xyzq.simpson.maggie.framework.dispatcher;

import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.helper.FileHelper;
import com.xyzq.simpson.base.logic.core.ICondition;
import com.xyzq.simpson.base.model.Path;
import com.xyzq.simpson.base.runtime.Kind;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.Array;
import com.xyzq.simpson.base.type.List;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.safe.Table;
import com.xyzq.simpson.base.xml.XMLNode;
import com.xyzq.simpson.base.xml.core.IXMLNode;
import com.xyzq.simpson.maggie.access.spring.MaggieAction;
import com.xyzq.simpson.maggie.framework.action.JavaAction;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.dispatcher.core.IActionDispatcher;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IClassActionLoadable;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IFileActionLoadable;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IJarActionLoadable;
import com.xyzq.simpson.maggie.framework.dispatcher.loadable.IStreamActionLoadable;
import com.xyzq.simpson.maggie.utility.ClassLoaderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Maggie动作分发器
 */
public class MaggieActionDispatcher extends ActionDispatcher implements IClassActionLoadable, IStreamActionLoadable, IFileActionLoadable, IJarActionLoadable {
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(MaggieActionDispatcher.class);
    /**
     * 文件根目录
     */
    public File root = null;
    /**
     * 动作文件扫描规则
     */
    public String actionScan = null;
    /**
     * 扫描基础包
     */
    public String packageBase = null;
    /**
     * 子分发器列表
     */
    protected Array<IActionDispatcher> dispatchers = null;
    /**
     * 控制器映射，uri映射动作实际路径
     */
    public Table<String, String> accessors = null;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        dispatchers = new Array<IActionDispatcher>(3);
        IActionDispatcher dispatcher = null;
        dispatcher = new TemplateActionDispatcher();
        ((TemplateActionDispatcher) dispatcher).root = this.root;
        if(!dispatcher.initialize()) {
            return false;
        }
        dispatchers.set(0, dispatcher);
        dispatcher = new JavaActionDispatcher();
        if(!dispatcher.initialize()) {
            return false;
        }
        dispatchers.set(1, dispatcher);
        dispatcher = new JSActionDispatcher();
        ((JSActionDispatcher) dispatcher).root = this.root;
        if(!dispatcher.initialize()) {
            return false;
        }
        dispatchers.set(2, dispatcher);
        accessors = new Table<String, String>();
        try {
            return loadAction();
        }
        catch (IOException ex) {
            throw new RuntimeException("maggie load action failed", ex);
        }
    }

    /**
     * 终止
     */
    @Override
    public void terminate() {
        accessors = null;
        for(IActionDispatcher dispatcher : dispatchers) {
            dispatcher.terminate();
        }
        dispatchers = null;
    }

    /**
     * 动作分发
     *
     * @param path 路径
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 是否分发到动作执行者
     */
    public boolean dispatch(String path, Visitor visitor, Context context) throws Exception {
        String result = accessors.get(path);
        if (null == result) {
            Path redirect = new Path(path, "/");
            while(true) {
                if (null == (redirect = redirect.roll("..", "/"))) {
                    return false;
                }
                String directory = redirect.toString("/") + "/";
                if (directory.startsWith("/")) {
                    directory = directory.substring(1);
                }
                result = accessors.get(directory + IActionDispatcher.ACTION_ROUTE + "." + IActionDispatcher.ACTION_SUFFIX);
                if (null != result) {
                    break;
                }
            }
        }
        while(true) {
            result = execute(result, visitor, context);
            if(null == result) {
                break;
            }
        }
        return true;
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
        int i = path.lastIndexOf(".");
        if(-1 == i) {
            logger.error("suffix not found in maggie execute, path = " + path);
            return null;
        }
        String postfix = path.substring(i + 1);
        for(IActionDispatcher dispatcher : dispatchers) {
            if(!dispatcher.suffixes().contains(postfix)) {
                continue;
            }
            return dispatcher.execute(path, visitor, context);
        }
        return null;
    }

    /**
     * 加载动作文件
     *
     * @return 执行结果
     */
    public boolean loadAction() throws IOException {
        // if(!loadLibrary(new File(root.getAbsolutePath() + File.separator + "library"))) {
        if(!loadLocal()) {
            logger.error("maggie load local action failed");
            return false;
        }
        if(!loadLibrary(root)) {
            logger.error("maggie load action library failed");
            return false;
        }
        if(!loadScript(root)) {
            logger.error("maggie load action script failed");
            return false;
        }
        logger.info("maggie load action success");
        return true;
    }

    /**
     * 加载本地动作类
     *
     * @return 执行结果
     */
    private boolean loadLocal() {
        if(Text.isBlank(packageBase)) {
            return true;
        }
        List<String> clazzList = null;
        try {
            clazzList = Kind.findClass(packageBase);
        }
        catch (IOException e) {
            logger.error("load package class failed, package = " + packageBase, e);
            return false;
        }
        for(String clazzName : clazzList) {
            if(clazzName.contains("$")) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(clazzName);
                if(!IAction.class.isAssignableFrom(clazz)) {
                    continue;
                }
                MaggieAction maggieAction = clazz.getAnnotation(MaggieAction.class);
                if(null == maggieAction) {
                    continue;
                }
                if(!loadAction(maggieAction.path(), (Class<IAction>) clazz)) {
                    continue;
                }
            }
            catch (ClassNotFoundException e) {
                logger.error("maggie load local class failed, className = " + clazzName, e);
            }
        }
        return true;
    }

    /**
     * 加载动作库文件
     *
     * @param file 库文件目录
     * @return 执行结果
     */
    private boolean loadLibrary(File file) throws IOException {
        Table<File, URLClassLoader> jarLoaderMap = new Table<File, URLClassLoader>();
        Stack<File> stack = new Stack<File>();
        stack.add(file);
        while(!stack.empty()) {
            File target = stack.pop();
            if(!target.exists()) {
                continue;
            }
            if(target.isDirectory()) {
                for(File item : target.listFiles()) {
                    if(item.isDirectory()) {
                        stack.add(item);
                    }
                }
                for(File item : target.listFiles()) {
                    if(!item.isDirectory()) {
                        stack.add(item);
                    }
                }
                continue;
            }
            if(!target.getName().endsWith(".jar")) {
                continue;
            }
            boolean sentry = false;
            for(ILink<File, URLClassLoader> link : jarLoaderMap) {
                if(FileHelper.contains(link.origin().getParentFile(), target) || FileHelper.contains(target.getParentFile(), link.origin())) {
                    // jarLoaderMap.put(target, link.destination());
                    ClassLoaderHelper.addURL(link.destination(), target.toURI().toURL());
                    sentry = true;
                    break;
                }
            }
            if(!sentry) {
                URLClassLoader classLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                ClassLoaderHelper.addURL(classLoader, target.toURI().toURL());
                jarLoaderMap.put(target, classLoader);
            }
        }
        for(ILink<File, URLClassLoader> link : jarLoaderMap) {
            if(!loadAction(new JarFile(link.origin()), link.destination())) {
                logger.error("load action jar failed, file = " + link.origin().getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    /**
     * 从Jar中加载动作
     *
     * @param jarFile Jar文件
     * @param classLoader 类加载器
     * @return 执行结果
     */
    @Override
    public boolean loadAction(JarFile jarFile, URLClassLoader classLoader) {
        File file = new File(jarFile.getName());
        URL url = null;
        try {
            url = file.toURI().toURL();
        }
        catch (MalformedURLException e) { }
        if(null == classLoader) {
            classLoader = new URLClassLoader(new URL[] {url}, MaggieActionDispatcher.class.getClassLoader());
        }
        else {
            ClassLoaderHelper.addURL(classLoader, url);
        }
        // 加载映射
        Table<String, String> classTable = new Table<String, String>();
        if(!Text.isBlank(actionScan)) {
            try {
                Enumeration<JarEntry> iterator = jarFile.entries();
                while(iterator.hasMoreElements()) {
                    JarEntry jarEntry = iterator.nextElement();
                    if(jarEntry.isDirectory()) {
                        continue;
                    }
                    if(jarEntry.getName().matches(actionScan)) {
                        InputStream inputStream = jarFile.getInputStream(jarEntry);
                        String text = Text.loadStream(inputStream, Encoding.ENCODING_UTF8);
                        inputStream.close();
                        XMLNode xmlNode = XMLNode.convert(text);
                        for(IXMLNode actionNode : xmlNode.visits("action")) {
                            classTable.put(actionNode.get("class"), actionNode.get("uri"));
                        }
                    }
                    else if(jarEntry.getName().matches("/?action/.*")) {
                        loadAction(jarEntry.getName().substring("action/".length()), jarFile.getInputStream(jarEntry), Encoding.ENCODING_UTF8, classLoader);
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("load action mapping failed, file = " + file.getAbsolutePath(), e);
            }
            // 加载Class
            try {
                final Table<String, String> finalClassTable = classTable;
                Kind.visitClassOfJar(file, classLoader, new ICondition<Class<?>>() {
                    @Override
                    public boolean check(Class<?> target) {
                        String uri = finalClassTable.get(target.getName());
                        if(null != uri) {
                            loadAction(uri, (Class<IAction>) target);
                        }
                        else {
                            MaggieAction maggieAction = target.getAnnotation(MaggieAction.class);
                            if(null != maggieAction) {
                                loadAction(maggieAction.path(), (Class<IAction>) target);
                            }
                        }
                        return true;
                    }
                });
            }
            catch (Exception e) {
                throw new RuntimeException("load action class failed, file = " + file.getAbsolutePath(), e);
            }
        }
        return true;
    }

    /**
     * 加载脚本动作文件或者目录
     *
     * @param file 文件或者目录
     * @return 执行结果
     */
    public boolean loadScript(File file) throws IOException {
        if(file.exists() && file.isDirectory()) {
            for(File item : file.listFiles()) {
                if(!loadScript(item)) {
                    logger.error("load script failed, file = " + item.getAbsolutePath());
                    return false;
                }
            }
            return true;
        }
        String path = "";
        if(!file.equals(root)) {
            path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
        }
        path = path.replace("\\", "/");
        return loadAction(path, file);
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
        int i = path.lastIndexOf(".");
        if(-1 == i) {
            logger.warn("load action file failed, path = " + path + ", file = " + file.getAbsolutePath());
            return true;
        }
        String suffix = path.substring(i + 1);
        String uri = path.substring(0, i).replace("\\", "/");
        for(IActionDispatcher dispatcher : dispatchers) {
            if(!dispatcher.suffixes().contains(suffix)) {
                continue;
            }
            if(IFileActionLoadable.class.isAssignableFrom(dispatcher.getClass())) {
                if(!((IFileActionLoadable) dispatcher).loadAction(path, file)) {
                    return false;
                }
                accessors.put(uri, path);
            }
        }
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
        int i = path.lastIndexOf(".");
        if(-1 == i) {
            return false;
        }
        String suffix = path.substring(i + 1);
        String uri = path.substring(0, i);
        for(IActionDispatcher dispatcher : dispatchers) {
            if(!dispatcher.suffixes().contains(suffix)) {
                continue;
            }
            if(IStreamActionLoadable.class.isAssignableFrom(dispatcher.getClass())) {
                if(!((IStreamActionLoadable) dispatcher).loadAction(path, stream, encoding, classLoader)) {
                    return false;
                }
                if(!(dispatcher instanceof TemplateActionDispatcher) || null == accessors.get(uri)) {
                    accessors.put(uri, path);
                }
            }
        }
        return true;
    }

    /**
     * 加载JAVA动作类
     *
     * @param uri   路径
     * @param clazz 动作类
     * @return 执行结果
     */
    @Override
    public boolean loadAction(String uri, Class<IAction> clazz) {
        String path = clazz.getName() + "." + JavaAction.ACTION_SUFFIX;
        for(IActionDispatcher dispatcher : dispatchers) {
            if(IClassActionLoadable.class.isAssignableFrom(dispatcher.getClass())) {
                if(!((IClassActionLoadable) dispatcher).loadAction(path, clazz)) {
                    return false;
                }
                accessors.put(uri, path);
            }
        }
        return true;
    }
}
