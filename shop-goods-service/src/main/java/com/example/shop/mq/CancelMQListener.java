package com.example.shop.mq;

import com.example.constant.ShopCode;
import com.example.entity.MQEntity;
import com.example.shop.mapper.TradeGoodsMapper;
import com.example.shop.mapper.TradeGoodsNumberLogMapper;
import com.example.shop.mapper.TradeMqConsumerLogMapper;
import com.example.shop.pojo.*;
import com.example.shop.pojo.TradeMqConsumerLogExample.Criteria;

import lombok.extern.slf4j.Slf4j;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

import static com.alibaba.fastjson.JSON.parseObject;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
    messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Value("${mq.order.consumer.group.name}")
    private String groupName;

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeMqConsumerLogMapper mqConsumerLogMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public void onMessage(MessageExt messageExt) {

        String msgId = null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            //1. 解析消息内容
            msgId = messageExt.getMsgId();
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            body = new String(messageExt.getBody(), UTF_8);

            log.info("接受消息成功");

            //2. 查询消息消费记录
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            primaryKey.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);

            if (Objects.nonNull(mqConsumerLog)) {
                //3. 判断如果消费过...
                //3.1 获得消息处理状态
                //处理过...返回
                int status = mqConsumerLog.getConsumerStatus();

                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode() == status) {
                    log.info("消息:" + msgId + ",已经处理过");
                    return;
                }

                //正在处理...返回
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode() == status) {
                    log.info("消息:" + msgId + ",正在处理");
                    return;
                }

                //处理失败
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode() == status) {
                    //获得消息处理次数
                    Integer times = mqConsumerLog.getConsumerTimes();
                    if (times > 3) {
                        log.info("消息:" + msgId + ",消息处理超过3次,不能再进行处理了");
                        return;
                    }
                    //重试消费
                    handleMessageRetryConsume(mqConsumerLog);
                }

            } else {
                //4. 判断如果没有消费过,先插入一个消费流水，稍后更新
                mqConsumerLog = addNewConsumerLog(msgId, tags, keys, body);
            }

            //5. 回退库存, 同时更新MQ消费流水
            handleMessageSuccessConsume(body, mqConsumerLog);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("库存回退失败！MQ消费记录失败+1");
            handleFirstConsumerFail(msgId, tags, keys, body);
        }

    }

    private void handleFirstConsumerFail(String msgId, String tags, String keys, String body) {
        TradeMqConsumerLogKey pk = new TradeMqConsumerLogKey();
        pk.setMsgTag(tags);
        pk.setMsgKey(keys);
        pk.setGroupName(groupName);
        TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(pk);
        if (isNull(mqConsumerLog)) {
            //数据库未有记录
            mqConsumerLog = new TradeMqConsumerLog();
            mqConsumerLog.setMsgTag(tags);
            mqConsumerLog.setMsgKey(keys);
            mqConsumerLog.setGroupName(groupName);
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
            mqConsumerLog.setMsgBody(body);
            mqConsumerLog.setMsgId(msgId);
            //更新失败次数=1
            mqConsumerLog.setConsumerTimes(1);
            mqConsumerLogMapper.insert(mqConsumerLog);
        } else {
            //更新失败次数+1
            mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
            mqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
        }
    }

    private TradeMqConsumerLog addNewConsumerLog(String msgId, String tags, String keys, String body) {
        TradeMqConsumerLog mqConsumerLog;
        mqConsumerLog = new TradeMqConsumerLog();
        mqConsumerLog.setMsgTag(tags);
        mqConsumerLog.setMsgKey(keys);
        mqConsumerLog.setGroupName(groupName);
        mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
        mqConsumerLog.setMsgBody(body);
        mqConsumerLog.setMsgId(msgId);
        //失败次数，因为第一次消费，所以没有失败
        mqConsumerLog.setConsumerTimes(0);

        //将消息处理信息添加到数据库
        mqConsumerLogMapper.insert(mqConsumerLog);
        return mqConsumerLog;
    }

    private void handleMessageSuccessConsume(String body, TradeMqConsumerLog mqConsumerLog) {
        MQEntity message = parseObject(body, MQEntity.class);
        Long goodsId = message.getGoodsId();
        TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
        goods.setGoodsNumber(goods.getGoodsNumber() + message.getGoodsNum());
        goodsMapper.updateByPrimaryKey(goods);

        //6. 将消息的处理状态改为成功
        mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
        mqConsumerLog.setConsumerTimestamp(new Date());
        mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
        log.info("回退库存成功");
    }

    private void handleMessageRetryConsume(TradeMqConsumerLog mqConsumerLog) {
        mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

        //使用数据库乐观锁更新
        TradeMqConsumerLogExample example = new TradeMqConsumerLogExample();
        Criteria criteria = example.createCriteria();
        criteria.andMsgTagEqualTo(mqConsumerLog.getMsgTag());
        criteria.andMsgKeyEqualTo(mqConsumerLog.getMsgKey());
        criteria.andGroupNameEqualTo(groupName);
        criteria.andConsumerTimesEqualTo(mqConsumerLog.getConsumerTimes());
        int modifiedRows = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog, example);
        if (modifiedRows <= 0) {
            //未修改成功,其他线程并发修改
            log.info("并发修改,稍后处理");
        }
    }
}
