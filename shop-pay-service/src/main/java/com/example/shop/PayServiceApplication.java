package com.example.shop;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.example.utils.IDWorker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author apple
 */
@SpringBootApplication
@EnableAsync
@EnableDubboConfiguration
public class PayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }

}
