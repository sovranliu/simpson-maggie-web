package com.xyzq.simpson.maggie.framework;

import org.slf4j.LoggerFactory;

/**
 * 页面日志
 */
public class Logger {
    /**
     * 日志对象
     */
    protected static org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);


    /**
     * 打印追踪日志
     *
     * @param log 日志
     */
    public void trace(Object log) {
        if(null != log) {
            logger.trace(log.toString());
        }
    }

    /**
     * 打印调试日志
     *
     * @param log 日志
     */
    public void debug(Object log) {
        if(null != log) {
            logger.debug(log.toString());
        }
    }

    /**
     * 打印信息日志
     *
     * @param log 日志
     */
    public void info(Object log) {
        if(null != log) {
            logger.info(log.toString());
        }
    }

    /**
     * 打印告警日志
     *
     * @param log 日志
     */
    public void warn(Object log) {
        if(null != log) {
            logger.warn(log.toString());
        }
    }

    /**
     * 打印错误日志
     *
     * @param log 日志
     */
    public void error(Object log) {
        if(null != log) {
            logger.error(log.toString());
        }
    }
}
