package com.fengdis.spider.job;

import com.fengdis.spider.crawler.IpProxyXiCiCrawler;
import com.fengdis.spider.handler.MQConsumeMsgListenerProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 项目启动时执行的初始化任务
 */
@Slf4j
@Component
public class RunnerJob implements CommandLineRunner {

    @Override
    public void run(String... args){
        log.info("项目启动时，将西祠网的前10页代理保存进DB");
        for (int i = 1; i <= 10; i++) {
            IpProxyXiCiCrawler.getProxyIpFromXiciWebByPageNum(i);
        }
    }

    @Autowired
    private DefaultMQPushConsumer defaultMQPushConsumer;

    @Autowired
    private MQConsumeMsgListenerProcessor mqConsumeMsgListenerProcessor;

    @PostConstruct
    public void init() {
        defaultMQPushConsumer.registerMessageListener(mqConsumeMsgListenerProcessor);
        try {
            defaultMQPushConsumer.start();
            log.info("****************启动队列消费程序成功*****************");
        } catch (MQClientException e) {
            log.error("*****************启动队列消费程序异常****************");
        }

    }

    @PreDestroy
    public void shutdown(){
        defaultMQPushConsumer.shutdown();
    }
}
