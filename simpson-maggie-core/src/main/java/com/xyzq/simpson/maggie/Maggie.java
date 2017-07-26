package com.xyzq.simpson.maggie;

import com.xyzq.simpson.maggie.framework.dispatcher.MaggieActionDispatcher;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xyzq.simpson.maggie.logic.BridgeService;
import com.xyzq.simpson.maggie.logic.InvokeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局主控对象
 */
public class Maggie {
    /**
     * 日志对象
     */
    public static Logger logger = LoggerFactory.getLogger(Maggie.class);
    /**
     * 外部调用服务
     */
    private static InvokeService invokeService = null;
    /**
     * JS内置桥接服务
     */
    private static BridgeService bridgeService = null;
    /**
     * 动作分发器
     */
    private static MaggieActionDispatcher dispatcher = null;
    /**
     * 根目录
     */
    private static String root = null;
    /**
     * 扫描基础包
     */
    private static String packageBase = null;
    /**
     * 文件上传目录
     */
    private static String upload = null;


    /**
     * 初始化
     *
     * @param root 根目录
     * @param upload 文件上传目录
     * @param actionScan 动作扫描
     * @param packageBase 扫描包
     * @return 执行结果
     */
    public static boolean initialize(String root, String upload, String actionScan, String packageBase) {
        root = root.replace("/", File.separator);
        root = root.replace("\\", File.separator);
        if (!root.endsWith(File.separator)) {
            root = root + File.separator;
        }
        Maggie.root = root;
        upload = upload.replace("/", File.separator);
        upload = upload.replace("\\", File.separator);
        if (!upload.endsWith(File.separator)) {
            upload = upload + File.separator;
        }
        Maggie.upload = upload;
        invokeService = new InvokeService();
        if(!invokeService.initialize()) {
            return false;
        }
        bridgeService = new BridgeService();
        if(!bridgeService.initialize()) {
            return false;
        }
        try {
            dispatcher = new MaggieActionDispatcher();
            dispatcher.root = new File(root);
            dispatcher.actionScan = actionScan;
            dispatcher.packageBase = packageBase;
            if (!dispatcher.initialize()) {
                return false;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("initialize dispatcher failed", e);
        }
        logger.info("maggie initialize successfully");
        return true;
    }

    /**
     * 析构
     */
    public static void terminate() {
        if(null != dispatcher) {
            dispatcher.terminate();
            dispatcher = null;
        }
        if(null != bridgeService) {
            bridgeService.terminate();
            bridgeService = null;
        }
        if(null != invokeService) {
            invokeService.terminate();
            invokeService = null;
        }
    }

    /**
     * 属性
     */
    public static String root() {
        return root;
    }
    public static String upload() {
        return upload;
    }
    public static String actionScan() {
        return dispatcher.actionScan;
    }
    public static InvokeService invokeService() {
        return invokeService;
    }
    public static BridgeService bridgeService() {
        return bridgeService;
    }
    public static MaggieActionDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * 执行动作
     *
     * @param uri 动作路径
     * @param request Servlet请求
     * @param response Servlet反馈
     * @return 执行结果
     */
    public static boolean doAction(String uri, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Context context = null;
        Visitor visitor = null;
        boolean result = false;
        try {
            context = Context.build(request);
            visitor = Visitor.build(request, request.getSession(), response);
            result = doAction(uri, visitor, context);
        }
        catch (ServletException ex) {
            throw ex;
        }
        finally {
            if (null != context) {
                context.destroy();
            }
            if (null != visitor) {
                visitor.destroy();
            }
        }
        return result;
    }

    /**
     * 执行动作
     *
     * @param uri 动作路径
     * @param visitor 访客对象
     * @param context 上下文
     * @return 执行结果
     */
    public static boolean doAction(String uri, Visitor visitor, Context context) throws ServletException {
        try {
            return dispatcher.dispatch(uri, visitor, context);
        }
        catch (Exception e) {
            throw new ServletException("maggie dispatch failed, uri = " + uri, e);
        }
    }
}
