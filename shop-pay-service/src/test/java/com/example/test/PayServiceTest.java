package com.example.test;

import com.example.api.IPayService;
import com.example.constant.ShopCode;
import com.example.shop.PayServiceApplication;
import com.example.shop.pojo.TradePay;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PayServiceApplication.class)
public class PayServiceTest {
    public static final long ORDER_ID = 687089993197424640L;
    @Autowired
    private IPayService payService;

    /**
     * 仅生成支付订单
     */
    @Test
    public void createPayment() {
        TradePay tradePay = new TradePay();
        tradePay.setOrderId(ORDER_ID);
        tradePay.setPayAmount(new BigDecimal(880));
        payService.createPayment(tradePay);
    }

    /**
     * 生成支付订单之后调用的支付回调
     */
    @Test
    @SneakyThrows
    public void callbackPayment() {
        TradePay tradePay = new TradePay();
        //687817512800362496L来自于上面生成的ID值，在trade_pay表里查询出来并在下面关联
        tradePay.setPayId(688541033616777216L);
        tradePay.setOrderId(ORDER_ID);
        //支付状态，决定付钱这件事是否成功
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());

        /* 此方法应该被第三方支付平台，支付完成之后被调用 */
        payService.callbackPayment(tradePay);

        TimeUnit.SECONDS.sleep(7);
    }

}
