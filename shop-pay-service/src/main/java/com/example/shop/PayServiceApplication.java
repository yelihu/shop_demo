package com.example.shop;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import com.example.utils.IDWorker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@SpringBootApplication
@EnableDubboConfiguration
public class PayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }

    //@Bean
    //public IDWorker getBean() {
    //    return new IDWorker(1, 2);
    //}
    //
    //@Bean
    //public ThreadPoolTaskExecutor getThreadPool() {
    //    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //    //worker 4个
    //    executor.setCorePoolSize(4);
    //    //最大线程8个
    //    executor.setMaxPoolSize(8);
    //    //任务队列100个
    //    executor.setQueueCapacity(100);
    //    //线程保活一分钟
    //    executor.setKeepAliveSeconds(60);
    //
    //    executor.setThreadNamePrefix("Pool-A");
    //
    //    //拒绝策略，交给调用者线程、主线程
    //    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //
    //    executor.initialize();
    //
    //    return executor;
    //}

}
