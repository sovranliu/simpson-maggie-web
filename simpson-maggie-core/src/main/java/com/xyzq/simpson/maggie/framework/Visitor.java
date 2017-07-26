package com.xyzq.simpson.maggie.framework;

import com.xyzq.simpson.base.interaction.core.IWritable;
import com.xyzq.simpson.base.type.Table;
import com.xyzq.simpson.base.type.core.ITable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.zip.GZIPOutputStream;

/**
 * 页面访问者
 */
public class Visitor implements IWritable<String> {
    /**
     * 回覆
     */
    protected HttpServletResponse response = null;
    /**
     * 会话对象
     */
    protected HttpSession session = null;
    /**
     * Cookie信息
     */
    protected Table<String, String> cookie = new Table<String, String>();
    /**
     * IP地址
     */
    protected String ip = null;


    /**
     * 获取指定名称的属性
     *
     * @param name 属性名称
     * @return 属性值
     */
    public Object attribute(String name) {
        return session.getAttribute(name);
    }

    /**
     * 设置指定名称的属性
     *
     * @param name 属性名称
     * @param value 属性值
     */
    public void setAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }

    /**
     * 回覆对象属性
     *
     * @return 回复对象
     */
    public HttpServletResponse response() {
        return this.response;
    }

    /**
     * 会话对象属性
     *
     * @return 会话对象
     */
    public HttpSession session() {
        return this.session;
    }

    /**
     * 获取IP
     *
     * @return
     */
    public String ip() {
        return this.ip;
    }

    /**
     * 获取全部Cookie
     *
     * @return 全部Cookie
     */
    public ITable<String, String> cookie() {
        return this.cookie;
    }

    /**
     * 获取指定键的Cookie
     *
     * @param key 键
     * @return Cookie
     */
    public String cookie(String key) {
        return cookie.get(key);
    }

    /**
     * 设置Cookie
     *
     * @param key 键
     * @param value 值
     */
    public void setCookie(String key, String value) {
        setCookie(key, value , 0);
    }

    /**
     * 设置Cookie
     *
     * @param key 键
     * @param value 值
     * @param expiry 周期
     */
    public void setCookie(String key, String value, int expiry) {
        if(null == value) {
            Cookie cookie = new Cookie(key, null);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
        else {
            try {
                value = URLEncoder.encode(value, "utf-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException("setCookie(" + key + "," + value + ") failed", e);
            }
            Cookie cookie = new Cookie(key, value);
            cookie.setPath("/");
            if(0 == expiry) {
                cookie.setMaxAge(60 * 60 * 24 * 7);
            }
            else {
                cookie.setMaxAge(expiry);
            }
            response.addCookie(cookie);
        }
    }

    /**
     * 写入
     *
     * @param data 数据
     */
    public void write(String data) throws Exception {
        OutputStream outputStream = null;
        if(null != response.getHeader("Content-Encoding") && response.getHeader("Content-Encoding").contains("gzip")) {
            outputStream = new GZIPOutputStream(response.getOutputStream());
        }
        else {
            outputStream = response.getOutputStream();
        }
        String encoding = response.getCharacterEncoding();
        if(null == encoding) {
            encoding = "utf-8";
        }
        outputStream.write(data.getBytes(encoding));
    }

    /**
     * 添加头
     *
     * @param name 头名称
     * @param value 头值
     */
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    /**
     * 设置内容格式
     *
     * @param contentType 内容格式
     */
    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }


    /**
     * 销毁
     */
    public void destroy() {
        if(null != response) {
//            try {
//                response.getWriter().close();
//            }
//            catch (IOException e) {
//                throw new RuntimeException(e);
//            }
            response = null;
        }
    }

    /**
     * 构建页面访问者对象
     *
     * @param request 请求
     * @param session 会话
     * @param response 回覆
     * @return 页面访问者
     */
    public static Visitor build(HttpServletRequest request, HttpSession session, HttpServletResponse response) {
        Visitor result = new Visitor();
        result.session = session;
        if(null != request.getCookies()) {
            for(int i = 0; i < request.getCookies().length; i++) {
                String value = request.getCookies()[i].getValue();
                try {
                    value = java.net.URLDecoder.decode(request.getCookies()[i].getValue(), "UTF-8");
                }
                catch(Exception ex) { }
                value = value.replace("'", "");
                value = value.replace(";", "");
                value = value.replace("--", "");
                value = value.replace("*", "");
                result.cookie.put(request.getCookies()[i].getName(), value);
            }
        }
        result.response = response;
        result.ip = fetchIP(request);
        return result;
    }

    /**
     * 从请求中抽取IP
     *
     * @param request 请求
     * @return 真实IP
     */
    public static String fetchIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if(ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                }
                catch (UnknownHostException e) { }
                ipAddress= inet.getHostAddress();
            }
        }
        if(ipAddress != null && ipAddress.length() > 15) {
            if(ipAddress.indexOf(",") > 0){
                ipAddress = ipAddress.substring(0,ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }
}
