package com.itheima.shop.mq;

import com.alibaba.fastjson.JSON;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MQEntity;
import com.itheima.shop.mapper.TradeCouponMapper;
import com.itheima.shop.pojo.TradeCoupon;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.alibaba.fastjson.JSON.parseObject;
import static com.itheima.constant.ShopCode.SHOP_COUPON_UNUSED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.*;

/**
 * @author apple
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
    messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        //1. 解析消息内容
        MQEntity message = parseObject(new String(messageExt.getBody(), UTF_8), MQEntity.class);
        log.info("优惠券服务接收到消息");

        if (nonNull(message.getCouponId())) {
            //2. 查询优惠券信息
            TradeCoupon coupon = couponMapper.selectByPrimaryKey(message.getCouponId());
            //3.更改优惠券状态
            coupon.setUsedTime(null);
            coupon.setIsUsed(SHOP_COUPON_UNUSED.getCode());
            coupon.setOrderId(null);
            couponMapper.updateByPrimaryKey(coupon);
        }

        log.info("回退优惠券成功");

    }
}
