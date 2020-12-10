package com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.service;

import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.entity.OrderNewInfoEntity;
import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.mapper.OrderNewMapper;
import groovy.util.logging.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author snowalker
 * @version 1.0
 * @date 2019/3/23 10:53
 * @className OrderNewSerivce
 * @desc
 */
@Log4j2
@Service
public class OrderNewSerivce {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderNewSerivce.class);

    @Autowired
    private OrderNewMapper orderNewMapper;

    /**
     * 订单列表查询
     * @param orderInfo
     * @return
     */
    public List<OrderNewInfoEntity> queryOrderInfoList(OrderNewInfoEntity orderInfo) {
        return orderNewMapper.queryOrderInfoList(orderInfo);
    }

    /**
     * 订单列表查询--通过订单id
     * @param orderInfo
     * @return
     */
    public OrderNewInfoEntity queryOrderInfoByOrderId(OrderNewInfoEntity orderInfo) {
        return orderNewMapper.queryOrderInfoByOrderId(orderInfo);
    }

    /**
     * 订单入库
     * @param orderInfo
     * @return
     */
    public int addOrder(OrderNewInfoEntity orderInfo) {
        LOGGER.info("订单入库开始，orderinfo={}", orderInfo.toString());
        return orderNewMapper.addOrder(orderInfo);
    }

    /**
     * 订单列表-分页查询
     * @param orderInfo
     * @return
     */
    public List<OrderNewInfoEntity> queryOrderInfoPage(OrderNewInfoEntity orderInfo) {
        return orderNewMapper.queryOrderInfoPage(orderInfo);
    }
}
