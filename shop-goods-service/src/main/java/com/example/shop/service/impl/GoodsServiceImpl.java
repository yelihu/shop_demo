package com.example.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.IGoodsService;
import com.example.constant.ShopCode;
import com.example.entity.Result;
import com.example.exception.CastException;
import com.example.shop.mapper.TradeGoodsMapper;
import com.example.shop.mapper.TradeGoodsNumberLogMapper;
import com.example.shop.pojo.TradeGoods;
import com.example.shop.pojo.TradeGoodsNumberLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

import static java.util.Objects.isNull;

@Component
@Service(interfaceClass = IGoodsService.class)
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long goodsId) {
        if (Objects.isNull(goodsId)) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return goodsMapper.selectByPrimaryKey(goodsId);
    }

    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if (isNull(goodsNumberLog) || isNull(goodsNumberLog.getGoodsNumber()) || isNull(goodsNumberLog.getOrderId())
            || goodsNumberLog.getGoodsNumber() <= 0) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        if (goods.getGoodsNumber() < goodsNumberLog.getGoodsNumber()) {
            //库存不足
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        //减库存
        goods.setGoodsNumber(goods.getGoodsNumber() - goodsNumberLog.getGoodsNumber());
        goodsMapper.updateByPrimaryKey(goods);

        //记录库存操作日志
        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLogMapper.insert(goodsNumberLog);

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

}
