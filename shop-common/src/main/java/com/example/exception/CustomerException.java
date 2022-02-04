package com.example.exception;

import com.example.constant.ShopCode;

/**
 * 自定义异常
 */
public class CustomerException extends RuntimeException {

    private ShopCode shopCode;

    public CustomerException(ShopCode shopCode) {
        this.shopCode = shopCode;
    }
}
