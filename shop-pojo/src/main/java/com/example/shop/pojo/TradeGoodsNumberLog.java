package com.example.shop.pojo;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author apple
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeGoodsNumberLog extends TradeGoodsNumberLogKey implements Serializable {
    private Integer goodsNumber;

    private Date logTime;

    public TradeGoodsNumberLog goodsNumber(Integer goodsNumber) {
        this.goodsNumber = goodsNumber;
        return this;
    }

    public TradeGoodsNumberLog logTime(Date logTime) {
        this.logTime = logTime;
        return this;
    }

    public TradeGoodsNumberLog goodsId(Long goodsId) {
        super.setGoodsId(goodsId);
        return this;
    }

    public TradeGoodsNumberLog orderId(Long orderId) {
        super.setOrderId(orderId);
        return this;
    }

}
