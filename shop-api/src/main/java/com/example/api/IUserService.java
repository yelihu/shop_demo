package com.example.api;

import com.example.entity.Result;
import com.example.shop.pojo.TradeUser;
import com.example.shop.pojo.TradeUserMoneyLog;

public interface IUserService {
    TradeUser findOne(Long userId);

    Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog);
}
