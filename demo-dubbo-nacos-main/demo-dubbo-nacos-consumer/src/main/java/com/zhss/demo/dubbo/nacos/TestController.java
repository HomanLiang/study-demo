package com.zhss.demo.dubbo.nacos;

import com.zhss.demo.dubbo.nacos.api.ServiceA;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务--consumer--控制器
 *
 * @author Homan Liang
 * @date 2020/10/29
 */
@RestController
public class TestController {

    @Reference(version = "1.0.0",
            interfaceClass = ServiceA.class,
            cluster = "failfast")
    private ServiceA serviceA;

    /**
     * 问候
     * @param name
     * @return
     */
    @GetMapping("/greet")
    public String greet(String name) {
        return serviceA.greet(name);
    }

}
