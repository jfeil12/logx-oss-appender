package org.logx.adapter;

import org.logx.storage.StorageService;
import org.logx.core.AsyncEngine;

import java.util.concurrent.TimeUnit;

/**
 * 抽象基类，提供通用适配器的基本实现
 * 处理核心组件的初始化
 */
public abstract class AbstractUniversalAdapter implements UniversalOSSAdapter {
    protected AsyncEngine asyncEngine;
    protected StorageService s3Storage;
    private boolean started = false;
    
    @Override
    public void start() {
        if (started) {
            return;
        }
        
        try {
            if (asyncEngine != null) {
                asyncEngine.start();
            }
            
            started = true;
        } catch (Exception e) {
            throw new RuntimeException("Adapter start failed", e);
        }
    }
    
    @Override
    public void stop() {
        if (!started) {
            return;
        }
        
        try {
            if (asyncEngine != null) {
                asyncEngine.stop(5, TimeUnit.SECONDS);
            }
            
            // 不要在这里关闭存储服务，因为AsyncEngine会负责管理存储服务的生命周期
            // 存储服务将在AsyncEngine关闭时被正确关闭
            
            started = false;
        } catch (Exception e) {
            throw new RuntimeException("Adapter stop failed", e);
        }
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
}