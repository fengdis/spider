package com.fengdis.spider.crawler;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.spring.common.CrawlerCache;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import com.fengdis.spider.common.HttpConstants;
import com.fengdis.spider.domain.db.IpProxy;
import com.fengdis.spider.domain.db.User;
import com.fengdis.spider.domain.db.UserRepository;
import com.fengdis.spider.service.IpProxyService;
import com.fengdis.spider.util.JsonUtil;
import com.fengdis.spider.json.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.seimicrawler.xpath.JXDocument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 爬取知乎用户信息的爬虫类
 */
@Slf4j
@Crawler(name = "user-crawler", httpTimeOut = 10000)
public class UserCrawler extends BaseSeimiCrawler{

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IpProxyService ipProxyService;

    @Override
    public String[] startUrls() {
        return new String[]{ HttpConstants.ZHIHU_USER_BASEINFO_URL_PREFIX + USER_URL_TOKEN  + HttpConstants.ZHIHU_USER_INFO_SUFFIX };
    }

    @Override
    public String proxy() {
        IpProxy ipProxy = null;
        //IpProxy ipProxy = ipProxyService.getActiveProxyIp();
        if (ipProxy != null) {
            log.info("本次用的代理是: [{}:{}]", ipProxy.getIp(), ipProxy.getPort());
            return ipProxy.getType().toLowerCase() + "://" + ipProxy.getIp() + ":" + ipProxy.getPort();
        }
        log.info("由于没有一个可用的代理IP，因此用的是本机IP。注意可能会被加入黑名单。");
        return super.proxy();
    }

    @Override
    public void start(Response response) {
        log.info("正在爬取[{}]用户的基本信息...", USER_URL_TOKEN);
        JXDocument document = response.document();
        String zhihuUserInfoJson = document.selN("body").get(0).asElement().text();
        // 用户的简介信息中可能会有双引号，会影响json解析---手动删除简介信息
        zhihuUserInfoJson = JsonUtil.removeTheStringFieldValue(zhihuUserInfoJson, false, "headline", "gender");
        UserInfo userInfo = JsonUtil.string2Obj(zhihuUserInfoJson, UserInfo.class);
        User user = UserInfo.toEntity(userInfo);
        userRepository.save(user);
    }

    private static String USER_URL_TOKEN;

    /**
     * 从知乎，获取一个用户的信息
     * @param urlToken 该用户的token(每个知乎用户有唯一的url_token)
     */
    public static void getUserInfoFromZhihu(String urlToken) {
        USER_URL_TOKEN = urlToken;
        String url = HttpConstants.ZHIHU_USER_BASEINFO_URL_PREFIX + USER_URL_TOKEN + HttpConstants.ZHIHU_USER_INFO_SUFFIX;
        Request request = Request.build(url, "start");
        request.setCrawlerName("user-crawler");
        CrawlerCache.consumeRequest(request);
    }
}
