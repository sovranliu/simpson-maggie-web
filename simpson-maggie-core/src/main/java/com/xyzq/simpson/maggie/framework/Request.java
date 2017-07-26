package com.xyzq.simpson.maggie.framework;

import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.etc.Serial;
import com.xyzq.simpson.base.helper.FileHelper;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.type.Table;
import com.xyzq.simpson.base.type.core.ITable;
import com.xyzq.simpson.maggie.Maggie;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * 页面请求
 */
public class Request {
    /**
     * 请求方法类型
     */
    protected String method = null;
    /**
     * 请求路径
     */
    protected String path = null;
    /**
     * 请求主机名
     */
    protected String host = null;
    /**
     * 请求头
     */
    protected ITable<String, String> header = null;
    /**
     * 查询参数
     */
    protected ITable<String, String[]> query = null;
    /**
     * 表单参数
     */
    protected ITable<String, Object[]> form = null;
    /**
     * 请求
     */
    protected HttpServletRequest request = null;


    /**
     * 请求方法类型属性
     *
     * @return 请求方法类型
     */
    public String method() {
        return method;
    }
    /**
     * 请求路径属性
     *
     * @return 请求路径
     */
    public String path() {
        return path;
    }
    /**
     * 请求协议schema
     *
     * @return 协议schema
     */
    public String schema() {
        return Text.substring(request.getRequestURL().toString(), null, ":");
    }
    /**
     * 请求主机名属性
     *
     * @return 请求主机名
     */
    public String host() {
        return host;
    }
    /**
     * 请求主机根路径
     *
     * @return 主机根路径
     */
    public String rootUrl() {
        int i = request.getRequestURL().indexOf(":");
        int j = request.getRequestURL().indexOf("//");
        if(-1 != j) {
            j += 2;
        }
        if(i < j) {
            i = j;
        }
        j = request.getRequestURL().indexOf("/", i);
        if(-1 == j) {
            return request.getRequestURL().toString();
        }
        else {
            return request.getRequestURL().substring(0, j);
        }
    }
    /**
     * 请求头属性
     *
     * @return 请求头
     */
    public ITable<String, String> header() {
        return header;
    }
    /**
     * 查询参数属性
     *
     * @return 查询参数
     */
    public ITable<String, String[]> query() {
        return query;
    }
    /**
     * 表单参数属性
     *
     * @return 表单参数
     */
    public ITable<String, Object[]> form() {
        return form;
    }
    /**
     * 请求属性
     *
     * @return 请求
     */
    public HttpServletRequest request() {
        return request;
    }

    /**
     * 请求的内容类型
     *
     * @return 请求的内容类型
     */
    public String contentType() {
        return request.getContentType();
    }

    /**
     * 获取查询字符串
     *
     * @return 查询字符串
     */
    public String queryString() {
        return request.getQueryString();
    }

    /**
     * 获取带参数的路径
     *
     * @return 带参数的路径
     */
    public String url() {
        return request.getRequestURL().toString();
    }

    /**
     * 获取带参数的路径
     *
     * @return 带参数的路径
     */
    public String uri() {
        return request.getRequestURI();
//        StringBuilder builder = new StringBuilder();
//        builder.append(path);
//        boolean sentry = false;
//        for(ILink<String, String> link : this.queryString) {
//            if(sentry) {
//                builder.append("&");
//            }
//            else {
//                sentry = true;
//                builder.append("?");
//            }
//            builder.append(link.origin());
//            builder.append("=");
//            builder.append(link.destination());
//        }
//        return builder.toString();
    }

    /**
     * 解析Servlet请求
     *
     * @param request Servlet请求
     * @return 执行结果
     */
    public boolean parse(HttpServletRequest request) {
        this.method = request.getMethod();
        this.path = request.getRequestURI().substring(1);
        this.host = request.getServerName();
        // 请求头
        this.header = new Table<String, String>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            this.header.put(key, value);
        }
        // 查询参数
        if(null != request.getContentType() && request.getContentType().startsWith("text/")) {
            try {
                this.query = new Table<String, String[]>();
                for(Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                    this.query.put(entry.getKey(), entry.getValue());
                }
                Object[] valueArray = new Object[1];
                valueArray[0] = Text.loadStream(request.getInputStream(), Encoding.ENCODING_UTF8);
                this.form = new Table<String, Object[]>();
                this.form.put("", valueArray);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        else if("application/x-www-form-urlencoded".equalsIgnoreCase(request.getContentType())) {
            this.query = parseQueryString(request.getQueryString());
            this.form = new Table<String, Object[]>();
            for(Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                Object[] formValue = this.form.get(entry.getKey());
                String[] valueArray = this.query.get(entry.getKey());
                if(null == valueArray) {
                    valueArray = new String[0];
                }
                for(String value : entry.getValue()) {
                    boolean sentry = false;
                    for(String item : valueArray) {
                        if(value.equals(item)) {
                            sentry = true;
                            break;
                        }
                    }
                    if(sentry) {
                        continue;
                    }
                    if(null == formValue) {
                        formValue = new String[1];
                    }
                    else {
                        String[] tmp = new String[formValue.length + 1];
                        System.arraycopy(formValue, 0, tmp, 0, formValue.length);
                        formValue = tmp;
                    }
                    formValue[formValue.length - 1] = value;
                }
                this.form.put(entry.getKey(), formValue);
            }
        }
        else if(null != request.getContentType() && request.getContentType().contains("multipart")) {
            this.query = new Table<String, String[]>();
            for(Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                this.query.put(entry.getKey(), entry.getValue());
            }
            this.form = parseMultiPart(request);
        }
        else {
            this.query = new Table<String, String[]>();
            for(Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                this.query.put(entry.getKey(), entry.getValue());
            }
            this.form = new Table<String, Object[]>();
        }
        this.request = request;
        return true;
    }

    /**
     * 解析查询字符串
     *
     * @param queryString 查询字符串
     * @return 查询参数
     */
    private Table<String, String[]> parseQueryString(String queryString) {
        Table<String, String[]> result = new Table<String, String[]>();
        if(null == queryString) {
            return result;
        }
        for(String pair : queryString.split("&")) {
            int i = pair.indexOf("=");
            if (-1 == i) {
                String[] valueArray = result.get(pair);
                if (null == valueArray) {
                    valueArray = new String[1];
                    result.put(pair, valueArray);
                } else {
                    String[] tmp = new String[valueArray.length + 1];
                    System.arraycopy(valueArray, 0, tmp, 0, valueArray.length);
                    valueArray = tmp;
                    result.put(pair, valueArray);
                }
                valueArray[valueArray.length - 1] = "";
            } else if (i > 0) {
                if (!pair.equals(Text.repeat("=", pair.length()))) {
                    String key = pair.substring(0, i);
                    String value = pair.substring(i + 1);
                    String[] valueArray = result.get(key);
                    if (null == valueArray) {
                        valueArray = new String[1];
                        result.put(key, valueArray);
                    } else {
                        String[] tmp = new String[valueArray.length + 1];
                        System.arraycopy(valueArray, 0, tmp, 0, valueArray.length);
                        valueArray = tmp;
                        result.put(key, valueArray);
                    }
                    valueArray[valueArray.length - 1] = value;
                }
            }
        }
        return result;
    }

    /**
     * 解析多段参数
     *
     * @param request 请求
     * @return 参数
     */
    private Table<String, Object[]> parseMultiPart(HttpServletRequest request) {
        Table<String, Object[]> result = new Table<String, Object[]>();
        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setHeaderEncoding("UTF-8");
            List<FileItem> fileList = upload.parseRequest(request);
            for(FileItem fileItem : fileList) {
                if(null != fileItem.getName()) {
                    String directoryPath = Maggie.upload() + Serial.makeLoopLong();
                    File directoryFile = new File(directoryPath);
                    if(directoryFile.exists()) {
                        FileHelper.delete(directoryFile);
                    }
                    directoryFile.mkdirs();
                    File file = new File(directoryPath + File.separator + fileItem.getName());
                    fileItem.write(file);
                    Object[] values = result.get(fileItem.getFieldName());
                    if(null == values) {
                        values = new Object[1];
                    }
                    else {
                        String[] tmp = new String[values.length + 1];
                        System.arraycopy(values, 0, tmp, 0, values.length);
                        values = tmp;
                    }
                    values[values.length - 1] = file;
                    result.put(fileItem.getFieldName(), values);
                }
                else if(null != fileItem.getString("UTF-8")) {
                    Object[] values = result.get(fileItem.getFieldName());
                    if(null == values) {
                        values = new Object[1];
                    }
                    else {
                        String[] tmp = new String[values.length + 1];
                        System.arraycopy(values, 0, tmp, 0, values.length);
                        values = tmp;
                    }
                    values[values.length - 1] = fileItem.getString("UTF-8");
                    result.put(fileItem.getFieldName(), values);
                }
            }
        }
        catch(Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }
}
