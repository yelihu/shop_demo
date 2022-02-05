package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.example.api.IPayService;
import com.example.entity.Result;
import com.example.exception.CastException;
import com.example.shop.mapper.TradeMqProducerTempMapper;
import com.example.shop.mapper.TradePayMapper;
import com.example.shop.pojo.TradeMqProducerTemp;
import com.example.shop.pojo.TradePay;
import com.example.shop.pojo.TradePayExample;
import com.example.utils.IDWorker;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;

import static com.example.constant.ShopCode.*;
import static com.example.constant.ShopCode.SHOP_PAYMENT_PAY_ERROR;
import static com.example.constant.ShopCode.SHOP_SUCCESS;
import static java.util.Objects.isNull;

/**
 * 支付服务实现
 */
@Slf4j
@Component
@Service(interfaceClass = IPayService.class)
public class PayServiceImpl implements IPayService {

    @Autowired
    private TradePayMapper tradePayMapper;

    @Autowired
    private TradeMqProducerTempMapper mqProducerTempMapper;
    //
    //@Autowired
    //private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private IDWorker idWorker;

    @Value("${rocketmq.producer.group}")
    private String groupName;

    @Value("${mq.topic}")
    private String topic;

    @Value("${mq.pay.tag}")
    private String tag;

    @Override
    public Result createPayment(TradePay tradePay) {

        if (isNull(tradePay) || isNull(tradePay.getOrderId())) {
            CastException.cast(SHOP_REQUEST_PARAMETER_VALID);
        }
        //1.判断订单支付状态
        validateOrderPayStatus(tradePay);

        //2.设置订单的状态为未支付
        tradePay.setIsPaid(SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        //3.保存支付订单
        tradePay.setPayId(idWorker.nextId());
        tradePayMapper.insert(tradePay);

        return new Result(SHOP_SUCCESS.getSuccess(), SHOP_SUCCESS.getMessage());
    }

    /**
     * 判断订单支付状态, 已经支付则报错，否则后续继续支付订单
     */
    private void validateOrderPayStatus(TradePay tradePay) {
        TradePayExample example = new TradePayExample();
        TradePayExample.Criteria criteria = example.createCriteria();
        criteria.andOrderIdEqualTo(tradePay.getOrderId());
        //已经支付
        criteria.andIsPaidEqualTo(SHOP_PAYMENT_IS_PAID.getCode());
        int rows = tradePayMapper.countByExample(example);
        if (rows > 0) {
            CastException.cast(SHOP_PAYMENT_IS_PAID);
        }
    }

    /**
     * 支付回调
     *
     * @param payResult 支付结果
     */
    @Override
    public Result callbackPayment(TradePay payResult) {
        log.info("支付回调");
        //判断支付结果给出的支付状态
        if (Objects.equals(payResult.getIsPaid(), SHOP_ORDER_PAY_STATUS_IS_PAY.getCode())) {
            //更新数据库里的支付订单状态为已支付
            int affectedRows = updatePayStatusDB(payResult);
            if (affectedRows == 1) {
                //1. 创建支付成功的消息、将消息持久化数据库
                registerMqProducerTemp(payResult);
                //2. 在线程池中进行处理
                sendPaySuccessMessageAsync(payResult);
            }
            return new Result(SHOP_SUCCESS.getSuccess(), SHOP_SUCCESS.getMessage());
        } else {
            CastException.cast(SHOP_PAYMENT_PAY_ERROR);
            return new Result(SHOP_FAIL.getSuccess(), SHOP_FAIL.getMessage());
        }
    }

    /**
     * 将返回参数给出的id到数据库里面查询订单并按照支付结果将其支付状态（代表支付成功、失败结果）更新为已经支付
     *
     * @param payResult 支付回调的传参
     */
    private int updatePayStatusDB(TradePay payResult) {
        TradePay pay = tradePayMapper.selectByPrimaryKey(payResult.getPayId());
        if (isNull(pay)) {
            CastException.cast(SHOP_PAYMENT_NOT_FOUND);
        }
        pay.setIsPaid(SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        log.info("支付订单状态改为已支付");
        //返回影响行数
        return tradePayMapper.updateByPrimaryKeySelective(pay);
    }

    /**
     * 多线程方式发送支付消息，多线程处理防止消息堆积
     */
    @Async("payThreadPool")
    public void sendPaySuccessMessageAsync(TradePay tradePay) {
        //threadPoolTaskExecutor.submit(() -> {
        //
        //});

        //发送消息到MQ
        SendResult result = null;
        try {
            result = sendMessage(topic, tag, String.valueOf(tradePay.getPayId()), JSON.toJSONString(tradePay));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("消息发送失败！！订单编号:{}", tradePay.getPayId());
        }
        //等待发送结果,如果MQ接受到消息,删除发送成功的消息
        if (result != null && Objects.equals(result.getSendStatus(), SendStatus.SEND_OK)) {
            log.info("消息发送成功");
            mqProducerTempMapper.deleteByPrimaryKey(TradeMqProducerTemp.builder()
                .id(String.valueOf(idWorker.nextId()))
                .groupName(groupName)
                .msgTopic(topic)
                .msgTag(tag)
                .msgKey(String.valueOf(tradePay.getPayId()))
                .msgBody(JSON.toJSONString(tradePay))
                .createTime(new Date())
                .build()
                .getId());
            log.info("持久化的消息发送流水记录在数据库已清理！");
        }

    }

    /**
     * 消息发送流水落库
     */
    private void registerMqProducerTemp(TradePay tradePay) {
        mqProducerTempMapper.insert(TradeMqProducerTemp.builder()
            //全局流水号，使用分布式主键
            .id(String.valueOf(idWorker.nextId()))
            .groupName(groupName)
            .msgTopic(topic)
            .msgTag(tag)
            //msgKey一般为业务表示
            .msgKey(String.valueOf(tradePay.getPayId()))
            .msgBody(JSON.toJSONString(tradePay))
            .createTime(new Date())
            .build());
        log.info("将支付成功消息持久化到数据库");
    }

    /**
     * 发送支付成功消息
     *
     * @param topic
     * @param tag
     * @param key
     * @param body
     */
    private SendResult sendMessage(String topic, String tag, String key, String body)
    throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if (StringUtils.isEmpty(topic)) {
            CastException.cast(SHOP_MQ_TOPIC_IS_EMPTY);
        }
        if (StringUtils.isEmpty(body)) {
            CastException.cast(SHOP_MQ_MESSAGE_BODY_IS_EMPTY);
        }
        Message message = new Message(topic, tag, key, body.getBytes());
        return rocketMQTemplate.getProducer()
            .send(message);
    }
}
