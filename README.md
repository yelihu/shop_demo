# 商城模拟下单和支付的分布式demo——shop_demo

## QuickStart

**需要如下前置条件**

1. RocketMQ 集群，双主双从最好
2. Zookeeper集群+Dubbo
3. MySQL数据库和JDK

## 简介

主要为了学习和总结RocketMQ在分布式系统的当中的应用场景，使用Zookeeper+Dubbo+Spring Cloud 搭建的简易分布式交易Demo，实现基本的商品下单、库存扣减、优惠券使用，分布式事务使用TCC方式实现。

## 技术栈

1. Zookeeper+Dubbo+Spring Cloud
2. RocketMQ
