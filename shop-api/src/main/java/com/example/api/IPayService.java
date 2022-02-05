package com.example.api;

import com.example.entity.Result;
import com.example.shop.pojo.TradePay;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

public interface IPayService {

    /**
     * 支付
     *
     * @param tradePay 包含订单编号和支付金额
     */
    Result createPayment(TradePay tradePay);

    /**
     * 支付回调
     *
     * @param tradePay
     * @return
     */
    Result callbackPayment(TradePay tradePay)
    throws InterruptedException, RemotingException, MQClientException, MQBrokerException;

}
