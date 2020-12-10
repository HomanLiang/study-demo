package com.zhss.demo.dubbo.nacos.api;

/**
 * 服务接口
 *
 * @author Homan Liang
 * @date 2020/10/29
 */
public interface ServiceA {
    /**
     * 问候
     * @param name
     * @return
     */
    String greet(String name);
}
