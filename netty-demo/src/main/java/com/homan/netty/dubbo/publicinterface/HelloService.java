package com.homan.netty.dubbo.publicinterface;

/**
 * 这个是接口，是服务提供方和 服务消费方都需要
 *
 * @author Homan
 */
public interface HelloService {
    /**
     * hello 方法
     *
     * @param mes
     * @return
     */
    String hello(String mes);
}
