package com.itheima.shop.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * @author apple
 */
@Data
public class TradeCoupon implements Serializable {

    private Long couponId;

    private BigDecimal couponPrice;

    private Long userId;

    private Long orderId;

    private Integer isUsed;

    private Date usedTime;

}
