package com.snowalker.shardingjdbc.snowalker.demo;

import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.constant.DbAndTableEnum;
import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.entity.OrderNewInfoEntity;
import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.sequence.KeyGenerator;
import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.service.OrderNewSerivce;
import com.snowalker.shardingjdbc.snowalker.demo.entity.OrderInfo;
import com.snowalker.shardingjdbc.snowalker.demo.service.OrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SnowalkerShardingjdbcDemoApplicationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowalkerShardingjdbcDemoApplicationTests.class);

    @Resource(name = "orderService")
    OrderService orderService;

    @Autowired
    KeyGenerator keyGenerator;

    @Test
    public void testInsertOrderInfo() {
        Random random = new Random();

        for (int i = 0; i < 1000; i++) {
            long userId = 4*i;
//            long orderId = keyGenerator.getSnowFlakeId();
            int incr = random.nextInt(2);
            long orderId = 4*i + incr;
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserName("snowalker");
            orderInfo.setUserId(userId);
            orderInfo.setOrderId(orderId);
            int result = orderService.addOrder(orderInfo);
            if (1 == result) {
                LOGGER.info("入库成功,orderInfo={}", orderInfo);
            } else {
                LOGGER.info("入库失败,orderInfo={}", orderInfo);
            }
        }
    }

    /**
     * 默认规则跨片归并
     */
    @Test
    public void testQueryList() {
        List<OrderInfo> list = new ArrayList<>();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(3l);
        orderInfo.setUserId(0l);
        list = orderService.queryOrderInfoList(orderInfo);
        LOGGER.info("结果==" + list.toString());
    }


    @Test
    public void testQueryById() {
        OrderInfo queryParam = new OrderInfo();
        queryParam.setUserId(8l);
        queryParam.setOrderId(8l);
        OrderInfo queryResult = orderService.queryOrderInfoByOrderId(queryParam);
        if (queryResult != null) {
            LOGGER.info("查询结果:orderInfo={}", queryResult);
        } else {
            LOGGER.info("查无此记录");
        }
    }

    /**
     * 测试分布式主键生成
     */
    @Test
    public void testGenerateId() {
        // 支付宝或者微信uid
        String outId = "1232132131241241243123";
        LOGGER.info("获取id开始");
        String innerUserId = keyGenerator.generateKey(DbAndTableEnum.T_USER, outId);
        LOGGER.info("外部id={},innerUserId={}", outId, innerUserId);
        String orderId = keyGenerator.generateKey(DbAndTableEnum.T_NEW_ORDER, innerUserId);
        LOGGER.info("外部id={},innerUserId={},orderId={}", outId, innerUserId, orderId);
    }


    @Autowired
    OrderNewSerivce orderNewSerivce;

    /**
     * 测试新的订单入库
     */
    @Test
    public void testNewOrderInsert() {
        // 支付宝或者微信uid
        for (int i = 0; i < 1; i++) {
            String outId = "1232132131241241243126" + i;
            LOGGER.info("获取id开始");
            String innerUserId = keyGenerator.generateKey(DbAndTableEnum.T_USER, outId);
            LOGGER.info("外部id={},内部用户={}", outId, innerUserId);
            String orderId = keyGenerator.generateKey(DbAndTableEnum.T_NEW_ORDER, innerUserId);
            LOGGER.info("外部id={},内部用户={},订单={}", outId, innerUserId, orderId);
            OrderNewInfoEntity orderInfo = new OrderNewInfoEntity();
            orderInfo.setUserName("snowalker");
            orderInfo.setUserId(innerUserId);
            orderInfo.setOrderId(orderId);
            orderNewSerivce.addOrder(orderInfo);
        }

    }

    /**
     * 测试订单明细查询
     */
    @Test
    public void testQueryNewOrderById() {
        String orderId = "OD000000012009281652219121200008";
        String userId = "UD020000012009281652207931200008";
        OrderNewInfoEntity orderInfo = new OrderNewInfoEntity();
        orderInfo.setOrderId(orderId);
        orderInfo.setUserId(userId);
        System.err.println(orderNewSerivce.queryOrderInfoByOrderId(orderInfo));
    }

    /**
     * 测试订单列表查询
     */
    @Test
    public void testQueryNewOrderList() {
        String userId = "UD020000012009281652207931200008";
        OrderNewInfoEntity orderInfo = new OrderNewInfoEntity();
        orderInfo.setUserId(userId);
        List<OrderNewInfoEntity> list = new ArrayList<>();
        list = orderNewSerivce.queryOrderInfoList(orderInfo);
        System.err.println(list);
    }

    /**
     * 分页测试
     */
    @Test
    public void testQueryOrderInfoPage() {
        OrderNewInfoEntity orderInfo = new OrderNewInfoEntity();
        String userId = "UD020000012009281652207931200008";
        orderInfo.setUserId(userId);
        List<OrderNewInfoEntity> list = new ArrayList<>();
        list = orderNewSerivce.queryOrderInfoPage(orderInfo);
        System.err.println(list);
    }
}
