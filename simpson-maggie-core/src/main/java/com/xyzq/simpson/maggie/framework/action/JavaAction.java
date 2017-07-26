package com.xyzq.simpson.maggie.framework.action;

import com.xyzq.simpson.bart.client.BartClient;
import com.xyzq.simpson.base.json.JSONArray;
import com.xyzq.simpson.base.json.JSONObject;
import com.xyzq.simpson.base.runtime.Kind;
import com.xyzq.simpson.lisa.Client;
import com.xyzq.simpson.lisa.spring.LisaService;
import com.xyzq.simpson.maggie.Maggie;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Java动作
 */
public class JavaAction extends Action {
    /**
     * 动作URI后缀
     */
    public final static String ACTION_SUFFIX = "java";
    /**
     * 实际动作类
     */
    public Class<IAction> clazz;


    /**
     * 初始化
     *
     * @return 是否初始化成功
     */
    @Override
    public boolean initialize() {
        Method method = null;
        try {
            method = clazz.getMethod("initialize");
        }
        catch (NoSuchMethodException e) { }
        if(null != method) {
            try {
                if(!(Boolean) method.invoke(null)) {
                    return false;
                }
            }
            catch (Exception ex) {
                throw new RuntimeException("invoke initialize method failed, class = " + clazz.getName() , ex);
            }
        }
        return true;
    }

    /**
     * 终止
     */
    @Override
    public void terminate() {
        Method method = null;
        try {
            method = clazz.getMethod("terminate");
        }
        catch (NoSuchMethodException e) { }
        if(null != method) {
            try {
                method.invoke(null);
            }
            catch (Exception ex) {
                throw new RuntimeException("invoke terminate method failed, class = " + clazz.getName() , ex);
            }
        }
    }

    /**
     * 动作执行
     *
     * @param visitor 访问者
     * @param context 请求上下文
     * @return 下一步动作，包括后缀名，null表示结束
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        String result = super.execute(visitor, context);
        if(null != result) {
            return result;
        }
        result = invoke(clazz, visitor, context);
        return result;
    }

    /**
     * 调用
     *
     * @param clazz 动作类
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作
     */
    public static String invoke(Class<IAction> clazz, Visitor visitor, Context context) throws Exception {
        IAction action = clazz.newInstance();
        for (Field field : Kind.fetchFields(clazz)) {
            if (null != field.getAnnotation(Autowired.class)) {
                field.setAccessible(true);
                field.set(action, Maggie.invokeService().getBean(field.getName()));
            }
            else if (null != field.getAnnotation(javax.annotation.Resource.class)) {
                field.setAccessible(true);
                field.set(action, Maggie.invokeService().getBean(field.getAnnotation(javax.annotation.Resource.class).name()));
            }
            if (null != field.getAnnotation(Value.class)) {
                Value value = field.getAnnotation(Value.class);
                String config = value.value();
                if(config.startsWith("${") && config.endsWith("}")) {
                    config = BartClient.instance().fetch(config.substring(2, config.length() - 1));
                }
                if(field.getType().equals(String.class)) {
                    field.setAccessible(true);
                    field.set(action, config);
                }
                else if(JSONObject.class.equals(field.getType())) {
                    field.setAccessible(true);
                    field.set(action, JSONObject.convertFromString(config));
                }
                else if(JSONArray.class.equals(field.getType())) {
                    field.setAccessible(true);
                    field.set(action, JSONArray.convertFromString(config));
                }
                else if(config.startsWith("{") && config.endsWith("}")) {
                    field.setAccessible(true);
                    field.set(action, JSONObject.convertFromString(config));
                }
                else if(config.startsWith("[") && config.endsWith("]")) {
                    field.setAccessible(true);
                    field.set(action, JSONArray.convertFromString(config));
                }
                else {
                    throw new RuntimeException("unexpected maggie action property type\nname = " + field.getName() + "\ntype= " + field.getType() + "\nconfig = " + config);
                }
            }
            if (null != field.getAnnotation(LisaService.class)) {
                String serviceName = field.getAnnotation(LisaService.class).name();
                field.setAccessible(true);
                field.set(action, Client.instance().loadService(serviceName, field.getType()));
            }
        }
        if(JavaAction.class.getClassLoader() != clazz.getClassLoader()) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                return action.execute(visitor, context);
            }
            finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
        else {
            return action.execute(visitor, context);
        }
    }
}
