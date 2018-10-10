package com.example.demo.bean;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.Brave.Builder;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.HttpSpanCollector;
import com.github.kristofa.brave.http.HttpSpanCollector.Config;
import com.github.kristofa.brave.httpclient.BraveHttpRequestInterceptor;
import com.github.kristofa.brave.httpclient.BraveHttpResponseInterceptor;
import com.github.kristofa.brave.servlet.BraveServletFilter;

@Configuration
public class ZipkinBean {

    /**
     * 配置收集器
     *
     * @return
     */
    @Bean
    public SpanCollector spanCollector() {
        Config config = HttpSpanCollector.Config.builder().compressionEnabled(false).connectTimeout(5000)
                .flushInterval(1).readTimeout(6000).build();
        return HttpSpanCollector.create("http://47.52.199.51:9411", config, new EmptySpanCollectorMetricsHandler());
    }

    /**
     * Brave各工具类的封装
     *
     * @param spanCollector
     * @return
     */
    @Bean
    public Brave brave(SpanCollector spanCollector) {
        Builder builder = new Builder("service2");// 指定serviceName
        builder.spanCollector(spanCollector);
        builder.traceSampler(Sampler.create(1));// 采集率
        return builder.build();
    }

    /**
     * 拦截器，需要serverRequestInterceptor,serverResponseInterceptor 分别完成sr和ss操作
     *
     * @param brave
     * @return
     */
    @Bean
    public BraveServletFilter braveServletFilter(Brave brave) {
        return new BraveServletFilter(brave.serverRequestInterceptor(), brave.serverResponseInterceptor(),
                new DefaultSpanNameProvider());
    }

    /**
     * httpClient客户端，需要clientRequestInterceptor,clientResponseInterceptor分别完成cs和cr操作
     *
     * @param brave
     * @return
     */
    @Bean
    public CloseableHttpClient httpClient(Brave brave) {
        CloseableHttpClient httpclient = HttpClients.custom()
                .addInterceptorFirst(new BraveHttpRequestInterceptor(brave.clientRequestInterceptor(),
                        new DefaultSpanNameProvider()))
                .addInterceptorFirst(new BraveHttpResponseInterceptor(brave.clientResponseInterceptor())).build();
        return httpclient;
    }
}