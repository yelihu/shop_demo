package com.example.shop.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class TradeOrder implements Serializable {
    private Long orderId;

    private Long userId;

    private Integer orderStatus;

    private Integer payStatus;

    private Integer shippingStatus;

    private String address;

    private String consignee;

    @NotNull(message = "商品编号不能为空")
    private Long goodsId;

    private Integer goodsNumber;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal goodsPrice;

    private Long goodsAmount;

    private BigDecimal shippingFee;

    private BigDecimal orderAmount;

    private Long couponId;

    private BigDecimal couponPaid;

    private BigDecimal moneyPaid;

    private BigDecimal payAmount;

    private Date addTime;

    private Date confirmTime;

    private Date payTime;

    /**
     * @return 订单总金额=单价 × 数量
     */
    public BigDecimal totalAmount() {
        return goodsPrice.multiply(new BigDecimal(goodsNumber));
    }

}
