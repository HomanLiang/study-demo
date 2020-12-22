package com.homan.netty.dubbo.customer;

import com.homan.netty.dubbo.netty.DubboClient;
import com.homan.netty.dubbo.publicinterface.HelloService;

/**
 * 客户端--入口
 *
 * @author Homan
 */
public class DubboClientBootstrap {

    /**
     * 这里定义协议头
     */
    public static final String providerName = "HelloService#hello#";

    public static void main(String[] args) throws Exception {
        // 创建一个消费者
        DubboClient customer = new DubboClient();
        // 创建代理对象
        HelloService service = (HelloService) customer.getBean(HelloService.class, providerName);

        for (; ; ) {
            Thread.sleep(2 * 1000);
            //通过代理对象调用服务提供者的方法(服务)
            String res = service.hello("你好 dubbo~");
            System.out.println("调用的结果 res= " + res);
        }
    }
}
