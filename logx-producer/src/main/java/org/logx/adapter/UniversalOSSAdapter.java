package org.logx.adapter;

/**
 * 通用适配器接口
 * 为不同日志框架提供统一的适配器接口
 */
public interface UniversalOSSAdapter {
    /**
     * 启动适配器
     */
    void start();
    
    /**
     * 处理日志事件
     * @param event 日志事件对象
     */
    void append(Object event);
    
    /**
     * 停止适配器
     */
    void stop();
    
    /**
     * 检查适配器是否已启动
     * @return true if started, false otherwise
     */
    boolean isStarted();
}