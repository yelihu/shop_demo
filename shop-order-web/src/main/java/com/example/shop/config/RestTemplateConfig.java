package com.example.shop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class RestTemplateConfig {

    @Bean
    @ConditionalOnMissingBean({RestOperations.class, RestTemplate.class})
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        RestTemplate restTemplate = new RestTemplate(factory);
        // 使用 utf-8 编码集的 convert 替换默认的 convert（默认的 string conver 的编码集为"ISO-8859-1"）
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        converters.removeIf(converter -> converter instanceof StringHttpMessageConverter);
        //等价于下面
        //while (iterator.hasNext()) {
        //    HttpMessageConverter<?> converter = iterator.next();
        //    if (converter instanceof StringHttpMessageConverter) {
        //        iterator.remove();
        //    }
        //}
        converters.add(new StringHttpMessageConverter(UTF_8));

        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean({ClientHttpRequestFactory.class})
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // ms
        factory.setReadTimeout(15000);
        // ms
        factory.setConnectTimeout(15000);
        return factory;
    }

}
