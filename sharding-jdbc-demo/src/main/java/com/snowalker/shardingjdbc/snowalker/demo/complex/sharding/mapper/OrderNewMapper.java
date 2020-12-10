package com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.mapper;

import com.snowalker.shardingjdbc.snowalker.demo.complex.sharding.entity.OrderNewInfoEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author snowalker
 * @version 1.0
 * @date 2019/3/23 10:52
 * @className OrderNewMapper
 * @desc 订单 new Mapper
 */
@Repository
public interface OrderNewMapper {
    /**
     * 订单列表查询
     * @param orderInfo
     * @return
     */
    List<OrderNewInfoEntity> queryOrderInfoList(OrderNewInfoEntity orderInfo);

    /**
     * 订单列表查询--通过订单id
     * @param orderInfo
     * @return
     */
    OrderNewInfoEntity queryOrderInfoByOrderId(OrderNewInfoEntity orderInfo);

    /**
     * 订单入库
     * @param orderInfo
     * @return
     */
    int addOrder(OrderNewInfoEntity orderInfo);

    /**
     * 订单列表-分页查询
     * @param orderInfo
     * @return
     */
    List<OrderNewInfoEntity> queryOrderInfoPage(OrderNewInfoEntity orderInfo);
}
