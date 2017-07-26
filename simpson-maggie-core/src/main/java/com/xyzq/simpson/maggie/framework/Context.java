package com.xyzq.simpson.maggie.framework;

import com.xyzq.simpson.base.json.JSONArray;
import com.xyzq.simpson.base.json.JSONObject;
import com.xyzq.simpson.base.json.JSONString;
import com.xyzq.simpson.base.json.core.IJSON;
import com.xyzq.simpson.base.model.core.IConverter;
import com.xyzq.simpson.base.type.Table;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.core.ITable;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * 页面请求上下文
 */
public class Context extends Request {
    /**
     * 日志对象
     */
    private static Logger logger = Logger.getLogger(Context.class);
    /**
     * 转换器接口
     */
    private static IConverter converter = null;
    /**
     * 上下文
     */
    protected Table<String, Object> context = new Table<String, Object>();
    /**
     * 模板上下文
     */
    protected VelocityContext template = new VelocityContext();


    /**
     * 隐藏构造函数
     */
    private Context() { }

    /**
     * 获取指定名称的参数
     *
     * @param key 参数名称
     * @return 参数值
     */
    public Object parameter(String key) {
        String[] stringArray = super.query.get(key);
        if(null != stringArray) {
            if(1 == stringArray.length) {
                return stringArray[0];
            }
            else {
                return stringArray; // Text.arrayToString(stringArray);
            }
        }
        Object[] objectArray = super.form.get(key);
        if(null != objectArray) {
            if(1 == objectArray.length) {
                return objectArray[0];
            }
            else {
                return objectArray;
            }
        }
        return null;
    }

    /**
     * 获取指定名称的参数
     */
    public Object parameter(String key, Object defaultValue) {
        Object result = parameter(key);
        if(null == result || "".equals(result)) {
            return defaultValue;
        }
        if(null == defaultValue) {
            return result;
        }
        return converter.convert(result, defaultValue.getClass());
    }

    /**
     * 获取参数集合映射
     *
     * @return 参数集合映射
     */
    public ITable<String, Object[]> parameters() {
        Table<String, LinkedList<Object>> table = new Table<String, LinkedList<Object>>();
        for(ILink<String, Object[]> link : super.form()) {
            LinkedList<Object> list = table.get(link.origin());
            if(null == list) {
                list = new LinkedList<Object>();
                table.put(link.origin(), list);
            }
            for(Object object : link.destination()) {
                list.add(object);
            }
        }
        for(ILink<String, String[]> link : super.query()) {
            LinkedList<Object> list = table.get(link.origin());
            if(null == list) {
                list = new LinkedList<Object>();
                table.put(link.origin(), list);
            }
            for(String string : link.destination()) {
                list.add(string);
            }
        }
        Table<String, Object[]> result = new Table<String, Object[]>();
        for(ILink<String, LinkedList<Object>> link : table) {
            LinkedList<Object> list = table.get(link.origin());
            result.put(link.origin(), list.toArray(new Object[0]));
        }
        return result;
    }

    /**
     * 模板上下文属性
     *
     * @return 模板上下文
     */
    public VelocityContext template() {
        return this.template;
    }

    /**
     * 获取上下文中的参数
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return context.get(key);
    }

    /**
     * 在上下文中设值
     *
     * @param key 键
     * @param value 值
     */
    public void put(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 在上下文中设值
     *
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        if(null == value) {
            template.remove(key);
        }
        else if(!template.containsKey(key)) {
            template.put(key, native2java(value));
        }
        else {
            template.remove(key);
            template.put(key, native2java(value));
        }
    }

    /**
     * 原生对象转Java对象
     *
     * @param nativeObject 原生对象
     * @return java对象
     */
    public static Object native2java(Object nativeObject) {
        if(null == nativeObject) {
            return null;
        }
        else if(nativeObject.getClass().getName().endsWith("NativeObject")) {
            return JSONObject.convertFromTable((Map) nativeObject);
        }
        else if(nativeObject.getClass().getName().endsWith("NativeArray")) {
            return JSONArray.convertFromSet((Collection) nativeObject);
        }
        else {
            return nativeObject;
        }
    }

    /**
     * 销毁
     */
    public void destroy() {
        for(ILink<String, Object[]> link : this.form) {
            if(null == link.destination()) {
                continue;
            }
            for(Object item : link.destination()) {
                if(item instanceof  File) {
                    ((File) item).delete();
                }
            }
        }
    }

    /**
     * 构建页面请求
     *
     * @param request 请求
     * @return 页面请求
     */
    public static Context build(HttpServletRequest request) {
        if(null == converter) {
            synchronized (Context.class) {
                if(null == converter) {
                    converter = JSONString.getConverter();
                }
            }
        }
        Context result = new Context();
        if(!result.parse(request)) {
            return null;
        }
        return result;
    }
}
