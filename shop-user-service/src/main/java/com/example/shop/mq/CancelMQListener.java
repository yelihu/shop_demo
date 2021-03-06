package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.api.IUserService;
import com.example.constant.ShopCode;
import com.example.entity.MQEntity;
import com.example.shop.pojo.TradeUserMoneyLog;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * @author apple
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
    messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private IUserService userService;

    @Override
    public void onMessage(MessageExt messageExt) {
        //1.解析消息
        MQEntity message = JSON.parseObject(new String(messageExt.getBody(), UTF_8), MQEntity.class);
        log.info("接收到消息");
        if (nonNull(message.getUserMoney()) && message.getUserMoney()
            .compareTo(BigDecimal.ZERO) > 0) {
            //2.调用业务层,进行余额修改
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setUseMoney(message.getUserMoney());
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            userMoneyLog.setUserId(message.getUserId());
            userMoneyLog.setOrderId(message.getOrderId());
            userService.updateMoneyPaid(userMoneyLog);
            log.info("余额回退成功");
        }
    }
}
