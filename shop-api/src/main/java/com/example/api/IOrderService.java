package com.example.api;

import com.example.entity.Result;
import com.example.shop.pojo.TradeOrder;

public interface IOrderService {

    /**
     * 下单接口
     *
     * @param order 订单
     * @return 下单业务处理的结果
     */
    Result confirmOrder(TradeOrder order);

}
