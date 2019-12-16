package com.fengdis.spider.crawler;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.spring.common.CrawlerCache;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import com.fengdis.spider.common.HttpConstants;
import com.fengdis.spider.common.MqConstants;
import com.fengdis.spider.domain.db.IpProxy;
import com.fengdis.spider.service.IpProxyService;
import com.fengdis.spider.handler.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 爬取可免费代理的服务器IP地址的爬虫类
 * 西刺网（https://www.xicidaili.com/wt/）
 */
@Slf4j
@Crawler(name = "proxy-ip-crawler", useUnrepeated = false)
public class IpProxyXiCiCrawler extends BaseSeimiCrawler {

    /*@Autowired
    private AmqpTemplate rabbitTemplate;*/

    @Autowired
    private DefaultMQProducer defaultMQProducer;
    @Autowired
    private IpProxyService ipProxyService;

    @Override
    public String getUserAgent() {
        return HttpConstants.refreshMyUserAgent();
    }

    @Override
    public String proxy() {
        IpProxy ipProxy = ipProxyService.getActiveProxyIp();
        if (ipProxy != null) {
            log.info("本次用的代理是: [{}:{}]", ipProxy.getIp(), ipProxy.getPort());
            return ipProxy.getType().toLowerCase() + "://" + ipProxy.getIp() + ":" + ipProxy.getPort();
        }
        log.info("由于没有一个可用的代理IP，因此用的是本机IP。注意可能会被加入黑名单。");
        return super.proxy();
    }

    @Override
    public String[] startUrls() {
        return new String[]{ HttpConstants.XICI_IP_PROXY_URL_PREFIX };
    }

    @Override
    public void start(Response response) {
        JXDocument jxDocument = response.document();
        JXNode node = jxDocument.selNOne("//*[@id=\"ip_list\"]");
        Elements ipProxyList = node.asElement().children().get(0).children();
        for(int i = 1; i < ipProxyList.size();i++) {
            Elements ipInfo = ipProxyList.get(i).children();
            String proxyIp = ipInfo.get(1).text();
            String proxyPort = ipInfo.get(2).text();
            String proxyAddress = ipInfo.get(3).text();
            String proxyType = ipInfo.get(5).text();

            IpProxy ipProxy = new IpProxy();
            ipProxy.setIp(proxyIp);
            ipProxy.setPort(proxyPort);
            ipProxy.setAddress(proxyAddress);
            ipProxy.setType(proxyType);
            // 将爬取到的代理放到消息队列中
            SendResult result = null;
            try {
                result = defaultMQProducer.send(new Message(MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB,MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB, Serialization.serialize(ipProxy)));
            } catch (Exception e) {
                log.error("生产异常",e);
            }
            //rabbitTemplate.convertAndSend(MqConstants.QUEUE_CHECK_PROXY_IP_AND_SAVE_TO_DB, ipProxy);
            log.info("发送结果:{}, msg:{}", result.getSendStatus(), result.toString());
        }
    }

    /**
     * 从西刺网获取更多的可用免费代理
     */
    public static void getProxyIpFromXiciWebByPageNum(Integer pageNum) {
        log.info("即将爬取西刺免费代理第{}页的代理IP...", pageNum);
        String url = HttpConstants.XICI_IP_PROXY_URL_PREFIX + pageNum;
        Request request = Request.build(url, "start");
        request.setCrawlerName("proxy-ip-crawler");
        CrawlerCache.consumeRequest(request);
    }
}
