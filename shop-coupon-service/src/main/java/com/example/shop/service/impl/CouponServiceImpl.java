package com.example.shop.service.impl;

import java.util.Objects;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.ICouponService;
import com.example.constant.ShopCode;
import com.example.entity.Result;
import com.example.exception.CastException;
import com.example.shop.mapper.TradeCouponMapper;
import com.example.shop.pojo.TradeCoupon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Service(interfaceClass = ICouponService.class)
public class CouponServiceImpl implements ICouponService {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public TradeCoupon findOne(Long coupouId) {
        if (Objects.isNull(coupouId)) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return couponMapper.selectByPrimaryKey(coupouId);
    }

    @Override
    public Result updateCouponStatus(TradeCoupon coupon) {
        if (Objects.isNull(coupon) || Objects.isNull(coupon.getCouponId())) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //更新优惠券状态
        couponMapper.updateByPrimaryKey(coupon);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }
}
