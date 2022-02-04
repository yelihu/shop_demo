package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.IUserService;
import com.example.constant.ShopCode;
import com.example.entity.Result;
import com.example.exception.CastException;
import com.example.shop.mapper.TradeUserMapper;
import com.example.shop.mapper.TradeUserMoneyLogMapper;
import com.example.shop.pojo.TradeUser;
import com.example.shop.pojo.TradeUserMoneyLog;
import com.example.shop.pojo.TradeUserMoneyLogExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import static java.util.Objects.isNull;

@Component
@Slf4j
@Service(interfaceClass = IUserService.class)
public class UserServiceImpl implements IUserService {

    @Autowired
    private TradeUserMapper userMapper;

    @Autowired
    private TradeUserMoneyLogMapper userMoneyLogMapper;

    @Override
    public TradeUser findOne(Long userId) {
        if (isNull(userId)) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return userMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog) {
        //1校验参数是否合法
        if (userMoneyLog == null || userMoneyLog.getUserId() == null || userMoneyLog.getOrderId() == null
            || userMoneyLog.getUseMoney() == null || userMoneyLog.getUseMoney()
            .compareTo(BigDecimal.ZERO) <= 0) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }

        //2查询订单余额使用日志
        TradeUserMoneyLogExample userMoneyLogExample = new TradeUserMoneyLogExample();
        TradeUserMoneyLogExample.Criteria criteria = userMoneyLogExample.createCriteria();
        criteria.andOrderIdEqualTo(Objects.requireNonNull(userMoneyLog)
            .getOrderId());
        criteria.andUserIdEqualTo(userMoneyLog.getUserId());
        long r = userMoneyLogMapper.countByExample(userMoneyLogExample);

        TradeUser user = userMapper.selectByPrimaryKey(userMoneyLog.getUserId());

        //3扣减余额
        if (userMoneyLog.getMoneyLogType()
            .intValue() == ShopCode.SHOP_USER_MONEY_PAID.getCode()
            .intValue()) {
            if (r > 0) {//已付款
                //已经付款
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //减余额 = 用户的余额 - 订单（流水对象）里面的钱
            long moneySpendThisTime = new BigDecimal(user.getUserMoney()).subtract(userMoneyLog.getUseMoney())
                .longValue();
            user.setUserMoney(moneySpendThisTime);
            userMapper.updateByPrimaryKey(user);
        }

        //4回退余额
        if (userMoneyLog.getMoneyLogType()
            .intValue() == ShopCode.SHOP_USER_MONEY_REFUND.getCode()
            .intValue()) {
            if (r < 0) {//未付款
                //如果没有支付,则不能回退余额
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY);
            }
            //防止多次退款
            TradeUserMoneyLogExample userMoneyLogExample1 = new TradeUserMoneyLogExample();
            TradeUserMoneyLogExample.Criteria criteria1 = userMoneyLogExample1.createCriteria();
            criteria1.andUserIdEqualTo(userMoneyLog.getUserId());
            criteria1.andOrderIdEqualTo(userMoneyLog.getOrderId());
            criteria1.andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            long r2 = userMoneyLogMapper.countByExample(userMoneyLogExample1);
            if (r2 > 0) {//已退过款
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
            }
            //退款 = 用户的余额 + 订单（流水对象）里面的钱
            user.setUserMoney(new BigDecimal(user.getUserMoney()).add(userMoneyLog.getUseMoney())
                .longValue());
            userMapper.updateByPrimaryKey(user);
        }

        //5记录订单余额使用日志
        userMoneyLog.setCreateTime(new Date());
        Result result;
        try {
            userMoneyLogMapper.insert(userMoneyLog);
            result = new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            log.info(e.toString());
            result = new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return result;
    }

}
