package com.xyzq.simpson.maggie.logic;

import com.xyzq.simpson.base.runtime.Invoke;
import com.xyzq.simpson.base.runtime.Method;
import com.xyzq.simpson.base.model.core.IFilter;
import com.xyzq.simpson.base.node.struct.Structure;
import com.xyzq.simpson.base.runtime.Kind;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.List;
import com.xyzq.simpson.base.type.safe.Table;
import com.xyzq.simpson.base.model.converter.JSConverter;

/**
 * 调用过滤器
 */
public class InvokeFilter implements IFilter<Object[], Invoke> {
    /**
     * 参数列表
     */
    private List<Kind> parameterTypes = new List<Kind>();
    /**
     * 参数索引与结构类名的映射
     */
    private Table<Integer, String> structureMap = new Table<Integer, String>();
    /**
     * 方法名称
     */
    private String methodName = null;


    /**
     * 获取参数个数
     *
     * @return 参数个数
     */
    public int parameterCount() {
        return this.parameterTypes.size();
    }

    /**
     * 获取方法名称
     *
     * @return 方法名称
     */
    public String methodName() {
        return this.methodName;
    }

    /**
     * 过滤
     *
     * @param origin 源数据
     * @return 目标数据结构
     */
    @Override
    public Invoke filter(Object[] origin) {
        Invoke result = new Invoke();
        result.method = new Method();
        result.method.name = methodName;
        result.method.parameterTypes = new Class<?>[origin.length];
        result.parameters = new Object[origin.length];
        int i = -1;
        for(Kind kind : parameterTypes) {
            i++;
            Object object = origin[i];
            if(null == object) {
                result.method.parameterTypes[i] = kind.clazz;
                result.parameters[i] = null;
            }
            else if(kind.equals(object.getClass())) {
                result.method.parameterTypes[i] = kind.clazz;
                result.parameters[i] = object;
            }
            else if(kind.equals(Object.class)) {
                result.method.parameterTypes[i] = kind.clazz;
                result.parameters[i] = object;
            }
            else {
                result.method.parameterTypes[i] = kind.clazz;
                result.parameters[i] = JSConverter.instance().convert(object, kind);
                String className = structureMap.get(i);
                if(null != className) {
                    ((Structure) result.parameters[i]).className = className;
                }
            }
        }
        return result;
    }

    /**
     * 构建调用过滤器
     *
     * @param functionString 函数字符串
     * @return 调用过滤器
     */
    public static InvokeFilter build(String functionString) {
        InvokeFilter result = new InvokeFilter();
        result.methodName = Text.substring(functionString, null, "(").trim();
        String parameterString = Text.substring(functionString, "(", ")");
        int i = -1;
        for(String parameter : Kind.splitGenerics(parameterString)) {
            i++;
            try {
                result.parameterTypes.add(Kind.build(parameter.trim()));
            }
            catch (ClassNotFoundException e) {
                result.parameterTypes.add(new Kind(Structure.class, null));
                result.structureMap.put(i, parameter.trim());
            }
        }
        return result;
    }

    /**
     * 转换为字符串
     *
     * @return 字符串
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(methodName);
        builder.append("(");
        boolean sentry = false;
        for(Kind parameterType : parameterTypes) {
            if(sentry) {
                builder.append(",");
            }
            else {
                sentry = true;
            }
            builder.append(parameterType.toString());
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * 比较
     *
     * @param target 比较目标
     * @return 比较结果
     */
    @Override
    public boolean equals(Object target) {
        if(null == target) {
            return false;
        }
        if(!target.getClass().equals(this.getClass())) {
            return false;
        }
        return toString().equals(target.toString());
    }
}
