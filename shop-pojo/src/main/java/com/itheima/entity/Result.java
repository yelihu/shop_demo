package com.itheima.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 结果实体类
 */
@Data
@AllArgsConstructor
public class Result implements Serializable {

    /**
     * 响应成功或失败
     */
    private Boolean success;

    /**
     * 响应内容
     */
    private String message;
}
