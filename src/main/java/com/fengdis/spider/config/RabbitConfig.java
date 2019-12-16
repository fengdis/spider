package com.fengdis.spider.config;

/**
 * 消息队列RabbitMQ的相关配置
 */
//@Configuration
public class RabbitConfig {

    /**
     * RabbitMQ队列（将可用代理保存进DB的队列，让Spring管理）
     */
    /*@Bean
    public Queue saveActiveProxyIpToDBQueue() {
        return new Queue(MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB, false);
    }

    @Bean
    public Exchange saveActiveProxyIpToDBExchange() {
        return new DirectExchange(MqConstants.EXCHANGE_CHECK_PROXY_IP_AND_SAVE_TO_DB);
    }

    @Bean
    public Binding saveActiveProxyIpToDBBinding() {
        return BindingBuilder
                .bind(saveActiveProxyIpToDBQueue())
                .to(saveActiveProxyIpToDBExchange())
                .with(MqConstants.KEY_CHECK_PROXY_IP_AND_SAVE_TO_DB)
                .noargs();
    }*/
}
