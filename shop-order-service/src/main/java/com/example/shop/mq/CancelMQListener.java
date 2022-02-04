package com.example.shop.mq;

import com.alibaba.fastjson.JSON;
import com.example.constant.ShopCode;
import com.example.entity.MQEntity;
import com.example.shop.mapper.TradeOrderMapper;
import com.example.shop.pojo.TradeOrder;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author apple
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
    messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeOrderMapper orderMapper;

    @Override
    public void onMessage(MessageExt messageExt) {

        //1. 解析消息内容
        MQEntity message = JSON.parseObject(new String(messageExt.getBody(), UTF_8), MQEntity.class);
        log.info("接受消息成功");
        //处理取消订单

        handleCancelOrder(message);

    }

    /**
     * 处理订单取消
     */
    private void handleCancelOrder(MQEntity message) {
        //2. 查询订单
        TradeOrder order = orderMapper.selectByPrimaryKey(message.getOrderId());
        //3.更新订单状态为取消
        order.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());
        orderMapper.updateByPrimaryKey(order);
        log.info("订单状态设置为取消成功！");
    }
}
