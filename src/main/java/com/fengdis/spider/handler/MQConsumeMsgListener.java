package com.fengdis.spider.handler;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @version 1.0
 * @Descrittion: RocketMQ消息消费监听
 * @author: fengdi
 * @since: 2019/08/28 17:26
 */
public class MQConsumeMsgListener implements MessageListenerConcurrently {
	
	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	
}
