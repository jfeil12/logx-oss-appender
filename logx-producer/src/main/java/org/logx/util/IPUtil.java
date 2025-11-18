package org.logx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP工具类
 * <p>
 * 提供获取本机IP地址的工具方法
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class IPUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(IPUtil.class);
    
    /**
     * 得到本机Ip
     *
     * @return 本机Ip
     */
    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("get local ip UnknownHostException", e);
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }
}