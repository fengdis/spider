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
import com.fengdis.spider.json.FollowInfo;
import com.fengdis.spider.service.IpProxyService;
import com.fengdis.spider.util.JsonUtil;
import com.fengdis.spider.json.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.seimicrawler.xpath.JXDocument;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 爬取被关注者信息的爬虫类（$rootName关注的用户信息）
 */
@Slf4j
@Crawler(name = "user-followee-crawler", useUnrepeated = false, httpTimeOut = 10000)
public class FolloweeCrawler extends BaseSeimiCrawler{

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IpProxyService ipProxyService;

    @Override
    public String getUserAgent() {
        return HttpConstants.refreshMyUserAgent();
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
    public void handleErrorRequest(Request request) {
        log.info("爬虫出现异常，继续发送爬虫请求直至爬取到该用户关注的所有知乎用户数据。");
        saveNextPageFolloweeInfo();
    }

    @Override
    public String[] startUrls() {
        if (USER_URL_TOKEN == null || "".equals(USER_URL_TOKEN)) {
            log.error("要爬取当前用户关注的知乎用户信息，需要传入该知乎用户的url_token信息，否则无法爬取数据...");
            return null;
        }
        log.info("正在爬取[{}]关注的知乎用户信息...", USER_URL_TOKEN);
        return new String[]{
                HttpConstants.ZHIHU_USER_BASEINFO_URL_PREFIX + USER_URL_TOKEN + "/followees" + HttpConstants.ZHIHU_USER_INFO_SUFFIX + "&limit=" + LIMIT + "&offset=" + OFFSET
        };
    }

    @Override
    public void start(Response response) {
        User followerUser = userRepository.findByUrlToken(USER_URL_TOKEN);
        if (followerUser == null) {
            log.error("要预先保存[{}]的用户信息，否则无法保证关联的关注关系", USER_URL_TOKEN);
            return;
        }
        JXDocument document = response.document();
        String followeeListJson = document.selN("body").get(0).asElement().text();
        // 爬取的知乎用户数据中，有些headline字段的值可能有双引号。删除内部的内容防止解析报错
        followeeListJson = JsonUtil.removeTheStringFieldValue(followeeListJson, false, "headline", "gender");
        FollowInfo followInfo = JsonUtil.string2Obj(followeeListJson, FollowInfo.class);
        Long totals = followInfo.getPaging().getTotals();
        log.info("要爬取当前用户关注的知乎用户总数量为：" + totals);
        if (totals == 0) {
            return;
        }
        List<UserInfo> userInfoList = followInfo.getData();
        List<User> userList = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            User user = UserInfo.toEntity(userInfo);
            userList.add(user);
        }
        userRepository.saveAll(userList);
        Integer hasGetTotal = OFFSET + LIMIT;
        if (hasGetTotal < totals) {
            log.info("已经爬取的数据条数[{}]，需要爬取的数据条数[{}]，因此还需要爬取下一页的数据", hasGetTotal, totals);
            OFFSET += LIMIT;
            saveNextPageFolloweeInfo();
        } else {
            log.info("已经爬取完[{}]关注的所有知乎用户的信息...", USER_URL_TOKEN);
        }
    }

    /**
     * 爬取下一页的被关注者信息
     */
    private void saveNextPageFolloweeInfo() {
        String url = HttpConstants.ZHIHU_USER_BASEINFO_URL_PREFIX + USER_URL_TOKEN + "/followees" + HttpConstants.ZHIHU_USER_INFO_SUFFIX + "&limit=" + LIMIT + "&offset=" + OFFSET;
        Request request = Request.build(url, "start");
        push(request);
    }

    private static String USER_URL_TOKEN;
    private static Integer LIMIT;
    private static Integer OFFSET;

    /**
     * 从知乎，获取一个用户的所有被关注者的信息
     * @param urlToken 该用户的token(每个知乎用户有唯一的url_token)
     * @param offset 起始位置
     */
    public static void getUserFolloweeInfoListFromZhihu(String urlToken, Integer offset) {
        USER_URL_TOKEN = urlToken;
        LIMIT = 20;
        OFFSET = offset;
        String url = HttpConstants.ZHIHU_USER_BASEINFO_URL_PREFIX + USER_URL_TOKEN + "/followees" + HttpConstants.ZHIHU_USER_INFO_SUFFIX + "&limit=" + LIMIT + "&offset=" + OFFSET;
        Request request = Request.build(url, "start");
        request.setCrawlerName("user-followee-crawler");
        CrawlerCache.consumeRequest(request);
    }
}
