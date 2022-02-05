package com.example.shop.pojo;

import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeMqProducerTemp implements Serializable {
    private String id;

    /**
     * MQ相关GROUP_NAME
     */
    private String groupName;
    /**
     * MQ相关Topic
     */
    private String msgTopic;
    /**
     * MQ相关Tag
     */
    private String msgTag;

    /**
     * 业务主键
     */
    private String msgKey;

    /**
     * 发送内容，一般为对象JSON字符串
     */
    private String msgBody;

    private Integer msgStatus;

    private Date createTime;

}
