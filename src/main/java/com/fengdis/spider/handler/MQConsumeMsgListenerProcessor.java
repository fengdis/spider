package com.fengdis.spider.handler;

import com.fengdis.spider.common.MqConstants;
import com.fengdis.spider.domain.db.IpProxy;
import com.fengdis.spider.service.IpProxyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @version 1.0
 * @Descrittion: RocketMQ消息消费监听
 * @author: fengdi
 * @since: 2019/08/28 17:26
 */
@Component
@Slf4j
public class MQConsumeMsgListenerProcessor extends MQConsumeMsgListener/*implements MessageListenerConcurrently */{

	AtomicLong sendTotal = new AtomicLong();
	AtomicLong sendFailTotal = new AtomicLong();

	@Autowired
	IpProxyService ipProxyService;

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		if(CollectionUtils.isEmpty(msgs)){
			log.info("接收到的消息为空，不做任何处理");
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}

		for (MessageExt messageExt : msgs) {
			sendTotal.incrementAndGet();
			if(messageExt.getTopic().equals(MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB)){
				if(messageExt.getTags().equals(MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB)){
					int reconsumeTimes = messageExt.getReconsumeTimes();
					if(reconsumeTimes == 3){
						return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
					}
					IpProxy ipProxy;
					try {
						ipProxy = Serialization.deserialize(messageExt.getBody(), IpProxy.class);
						log.info("消费响应:msgId:{},msgBody:{},tag:{},topic:{}", messageExt.getMsgId(), ipProxy.toString(), messageExt.getTags(), messageExt.getTopic());

						ipProxyService.checkProxyIpAndSaveToDB(ipProxy);
					} catch (Exception e) {
						log.error("消费异常");
						sendFailTotal.incrementAndGet();
						continue;
					}
				}
			}
		}

		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	
}
