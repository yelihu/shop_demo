package com.itheima.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.api.IUserService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeUserMapper;
import com.itheima.shop.mapper.TradeUserMoneyLogMapper;
import com.itheima.shop.pojo.TradeUser;
import com.itheima.shop.pojo.TradeUserMoneyLog;
import com.itheima.shop.pojo.TradeUserMoneyLogExample;
import com.itheima.shop.pojo.TradeUserMoneyLogExample.Criteria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

import static java.util.Objects.isNull;

@Component
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
        //1.校验对象、用户、订单、余额金额大小是否合法
        validateUserMoneyLog(userMoneyLog);
        Long orderId = userMoneyLog.getOrderId();
        Long userId = userMoneyLog.getUserId();

        //2.查询订单余额使用日志
        int recordNum = getTradeUserMoneyLogByOrderIdAndUserId(orderId, userId);
        TradeUser tradeUser = userMapper.selectByPrimaryKey(userMoneyLog.getUserId());

        //3.扣减余额...
        if (userMoneyLog.getMoneyLogType()
            .intValue() == ShopCode.SHOP_USER_MONEY_PAID.getCode()
            .intValue()) {
            if (recordNum > 0) {
                //已经付款
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //减余额 = 用户的余额 - 订单（流水对象）里面的钱
            long moneySpendThisTime = new BigDecimal(tradeUser.getUserMoney()).subtract(userMoneyLog.getUseMoney())
                .longValue();
            tradeUser.setUserMoney(moneySpendThisTime);
            userMapper.updateByPrimaryKey(tradeUser);
        }
        //4.回退余额...
        if (userMoneyLog.getMoneyLogType()
            .intValue() == ShopCode.SHOP_USER_MONEY_REFUND.getCode()
            .intValue()) {
            if (recordNum < 0) {
                //如果没有支付,则不能回退余额
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY);
            }
            //防止多次退款
            validateRefund(userMoneyLog);
            //退款 = 用户的余额 + 订单（流水对象）里面的钱
            tradeUser.setUserMoney(new BigDecimal(tradeUser.getUserMoney()).add(userMoneyLog.getUseMoney())
                .longValue());
            userMapper.updateByPrimaryKey(tradeUser);
        }
        //5.记录订单余额使用日志
        userMoneyLog.setCreateTime(new Date());
        userMoneyLogMapper.insert(userMoneyLog);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    /**
     * 校验退款条件
     */
    private void validateRefund(TradeUserMoneyLog userMoneyLog) {
        TradeUserMoneyLogExample logExample = new TradeUserMoneyLogExample();
        Criteria criteria1 = logExample.createCriteria();
        criteria1.andOrderIdEqualTo(userMoneyLog.getOrderId());
        criteria1.andUserIdEqualTo(userMoneyLog.getUserId());
        criteria1.andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
        int record = userMoneyLogMapper.countByExample(logExample);
        if (record > 0) {
            CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
        }
    }

    private int getTradeUserMoneyLogByOrderIdAndUserId(Long orderId, Long userId) {
        TradeUserMoneyLogExample example = new TradeUserMoneyLogExample();
        Criteria ctr = example.createCriteria();

        ctr.andOrderIdEqualTo(orderId);

        ctr.andUserIdEqualTo(userId);

        int r = userMoneyLogMapper.countByExample(example);
        return r;
    }

    private void validateUserMoneyLog(TradeUserMoneyLog userMoneyLog) {
        //对象、用户、订单、余额数量的校验
        if (isNull(userMoneyLog) || isNull(userMoneyLog.getUserId()) || isNull(userMoneyLog.getOrderId()) || isNull(
            userMoneyLog.getUseMoney()) || userMoneyLog.getUseMoney()
            .compareTo(BigDecimal.ZERO) <= 0) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
    }
}
