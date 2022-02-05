package com.example.shop.config;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.utils.IDWorker;

/**
 * TODO
 *
 * @author yelh25921@yunrong.cn
 * @version V3.0
 * @desc
 * @since 2022/2/5 9:12 PM
 */
@Configuration
public class PayServiceConfiguration {
    @Bean
    public IDWorker getBean() {
        return new IDWorker(1, 2);
    }

    @Bean
    public ThreadPoolTaskExecutor getThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //worker 4个
        executor.setCorePoolSize(4);
        //最大线程8个
        executor.setMaxPoolSize(8);
        //任务队列100个
        executor.setQueueCapacity(100);
        //线程保活一分钟
        executor.setKeepAliveSeconds(60);

        executor.setThreadNamePrefix("Pool-A");

        //拒绝策略，交给调用者线程、主线程
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());

        executor.initialize();

        return executor;
    }

}
