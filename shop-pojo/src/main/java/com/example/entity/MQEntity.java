package com.example.entity;

import java.math.BigDecimal;

import com.example.shop.pojo.TradeOrder;

import lombok.Data;

/**
 *
 */
@Data
public class MQEntity {
    /**
     * 订单编号
     */
    private Long orderId;
    /**
     * 优惠券编号
     */
    private Long couponId;
    /**
     * 用户编号
     */
    private Long userId;

    private BigDecimal userMoney;

    private Long goodsId;

    private Integer goodsNum;

    public MQEntity orderId(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    public MQEntity couponId(Long couponId) {
        this.couponId = couponId;
        return this;
    }

    public MQEntity userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public MQEntity userMoney(BigDecimal userMoney) {
        this.userMoney = userMoney;
        return this;
    }

    public MQEntity goodsId(Long goodsId) {
        this.goodsId = goodsId;
        return this;
    }

    public MQEntity goodsNum(Integer goodsNum) {
        this.goodsNum = goodsNum;
        return this;
    }

    /**
     * 封装订单消息
     */
    public static MQEntity initMqByOrder(Long orderId, TradeOrder order) {
        return new MQEntity().orderId(orderId)
            .userId(order.getUserId())
            .userMoney(order.getMoneyPaid())
            .goodsId(order.getGoodsId())
            .goodsNum(order.getGoodsNumber())
            .couponId(order.getCouponId());
    }

}
