package com.zhss.demo.dubbo.nacos;

import com.zhss.demo.dubbo.nacos.api.ServiceA;
import org.apache.dubbo.config.annotation.Service;

/**
 * 服务接口
 *
 * @author Homan Liang
 * @date 2020/10/29
 */
@Service(
        version = "1.0.0",
        interfaceClass = ServiceA.class,
        cluster = "failfast",
        loadbalance = "roundrobin"
)
public class ServiceAImpl implements ServiceA {
    /**
     * 问候
     * @param name
     * @return
     */
    @Override
    public String greet(String name) {
        return "hello, " + name;
    }
}
