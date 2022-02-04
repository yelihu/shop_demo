package com.itheima.shop.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.api.ICouponService;
import com.itheima.api.IGoodsService;
import com.itheima.api.IOrderService;
import com.itheima.api.IUserService;
import com.itheima.constant.ShopCode;
import com.itheima.entity.MQEntity;
import com.itheima.entity.Result;
import com.itheima.exception.CastException;
import com.itheima.shop.mapper.TradeOrderMapper;
import com.itheima.shop.pojo.*;
import com.itheima.utils.IDWorker;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

import static com.alibaba.fastjson.JSON.toJSONString;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@Service(interfaceClass = IOrderService.class)
public class OrderServiceImpl implements IOrderService {

    @Reference
    private IGoodsService goodsService;

    @Reference
    private IUserService userService;

    @Reference
    private ICouponService couponService;

    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.order.tag.cancel}")
    private String tag;

    @Autowired
    private TradeOrderMapper orderMapper;

    @Autowired
    private IDWorker idWorker;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public Result confirmOrder(TradeOrder order) {
        //1.校验订单
        checkOrder(order);
        //2.生成预订单，成功后在"6.确认订单"这一步设置预订单的成功
        Long orderId = savePreOrder(order);
        try {
            //3.扣减库存
            reduceGoodsNum(order);
            //4.扣减优惠券
            updateCouponStatus(order);
            //5.使用余额
            reduceMoneyPaid(order);
            //模拟异常抛出
            CastException.cast(ShopCode.SHOP_FAIL);
            //6.确认订单
            updateOrderStatus(order);
            //7.返回成功状态
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {

            //1.确认订单失败,发送消息
            MQEntity message = MQEntity.initMqByOrder(orderId, order);

            //2.返回订单确认失败消息
            try {
                sendCancelOrder(topic, tag, order.getOrderId()
                    .toString(), toJSONString(message));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     * 发送订单确认失败消息
     *
     * @param topic
     * @param tag
     * @param keys
     * @param body
     */
    private void sendCancelOrder(String topic, String tag, String keys, String body)
    throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(topic, tag, keys, body.getBytes());
        rocketMQTemplate.getProducer()
            .send(message);
        log.info("发送订单确认消息: {}", body);
    }

    /**
     * 确认订单
     *
     * @param order
     */
    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        order.setConfirmTime(new Date());
        int r = orderMapper.updateByPrimaryKey(order);
        if (r <= 0) {
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        log.info("订单:" + order.getOrderId() + "确认订单成功");
    }

    /**
     * 扣减余额
     */
    private void reduceMoneyPaid(TradeOrder order) {
        if (nonNull(order.getMoneyPaid()) && order.getMoneyPaid()
            .compareTo(ZERO) > 0) {
            //扣减余额流水
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setOrderId(order.getOrderId());
            userMoneyLog.setUserId(order.getUserId());
            userMoneyLog.setUseMoney(order.getMoneyPaid());
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());

            //更新余额
            Result result = userService.updateMoneyPaid(userMoneyLog);
            if (result.getSuccess()
                .equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            log.info("订单:" + order.getOrderId() + ",扣减余额成功");
        }
    }

    /**
     * 使用优惠券
     */
    private void updateCouponStatus(TradeOrder order) {
        if (nonNull(order.getCouponId())) {
            TradeCoupon coupon = couponService.findOne(order.getCouponId());
            coupon.setOrderId(order.getOrderId());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            coupon.setUsedTime(new Date());

            //更新优惠券状态
            Result result = couponService.updateCouponStatus(coupon);
            if (result.getSuccess()
                .equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
            log.info("订单:" + order.getOrderId() + ",使用优惠券");
        }
    }

    /**
     * 扣减库存
     */
    private void reduceGoodsNum(TradeOrder order) {
        //库存扣减流水记录
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog().orderId(order.getOrderId())
            .goodsId(order.getGoodsId())
            .goodsNumber(order.getGoodsNumber());

        Result result = goodsService.reduceGoodsNum(goodsNumberLog);

        if (result.getSuccess()
            .equals(ShopCode.SHOP_FAIL.getSuccess())) {
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }

        log.info("订单:" + order.getOrderId() + "扣减库存成功");
    }

    /**
     * 生成预订单
     *
     * @param order
     * @return
     */
    private Long savePreOrder(TradeOrder order) {
        //1. 设置订单状态为不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2. 设置订单ID
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        //3. 核算订单运费
        BigDecimal shippingFee = calcShippingFee(order.getOrderAmount());
        if (order.getShippingFee()
            .compareTo(shippingFee) != 0) {
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }

        //4. 核算订单总金额是否合法
        if (order.getOrderAmount()
            .compareTo(order.totalAmount()
                .add(shippingFee)) != 0) {
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }

        //5.判断用户是否使用余额
        if (nonNull(order.getMoneyPaid())) {
            //校验订余额
            validateOrderMoneyPaid(order);
        } else {
            order.setMoneyPaid(ZERO);
        }

        //6.判断用户是否使用优惠券
        if (nonNull(order.getCouponId())) {
            TradeCoupon coupon = validateCouponUsed(order);
            order.setCouponPaid(coupon.getCouponPrice());
        } else {
            order.setCouponPaid(ZERO);
        }

        //7.核算订单需要支付的金额 = 订单总金额-（账务余额+优惠券金额）
        order.setPayAmount(order.getOrderAmount()
            .subtract(order.getMoneyPaid())
            .subtract(order.getCouponPaid()));

        //8.设置下单时间
        order.setAddTime(new Date());
        //9.保存订单到数据库
        orderMapper.insert(order);
        //10.返回订单ID
        return orderId;
    }

    /**
     * 校验优惠券是否存在和已经使用
     */
    private TradeCoupon validateCouponUsed(TradeOrder order) {
        TradeCoupon coupon = couponService.findOne(order.getCouponId());
        //判断优惠券是否存在
        if (isNull(coupon)) {
            CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
        }
        //判断优惠券是否已经被使用
        if (requireNonNull(coupon).getIsUsed()
            .intValue() == ShopCode.SHOP_COUPON_ISUSED.getCode()
            .intValue()) {
            CastException.cast(ShopCode.SHOP_COUPON_ISUSED);
        }
        return coupon;
    }

    /**
     * 订单中余额是否合法
     *
     * @param order
     */
    private void validateOrderMoneyPaid(TradeOrder order) {
        BigDecimal moneyPaid = order.getMoneyPaid();
        //5.1
        int rest = moneyPaid.compareTo(ZERO);
        //余额小于0
        if (rest < 0) {
            CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
        }
        //余额大于0
        if (rest > 0) {
            TradeUser user = userService.findOne(order.getUserId());
            if (moneyPaid.compareTo(new BigDecimal(user.getUserMoney())) > 0) {
                CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
            }
        }
    }

    /**
     * 核算运费
     * 订单金额超过100元免运费，否则10元运费
     *
     * @param orderAmount
     * @return
     */
    private static BigDecimal calcShippingFee(BigDecimal orderAmount) {
        return orderAmount.compareTo(new BigDecimal("100")) > 0 ? ZERO : new BigDecimal("10");
    }

    /**
     * 校验订单是否存在、商品是否存在、订单是否存在、商品单价和数量是否合法？
     *
     * @param order
     */
    private void checkOrder(TradeOrder order) {
        //1.校验订单是否存在
        if (isNull(order)) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        TradeGoods goods = goodsService.findOne(order.getGoodsId());
        //2.校验订单中的商品是否存在
        if (isNull(goods)) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);

        }
        TradeUser user = userService.findOne(order.getUserId());
        //3.校验下单用户是否存在
        if (isNull(user)) {
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }

        //4.校验商品单价是否合法
        if (order.getGoodsPrice()
            .compareTo(requireNonNull(goods).getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }

        //5.校验订单商品数量是否合法，防止超卖
        if (order.getGoodsNumber() >= goods.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }

        log.info("校验订单通过");

    }

}
