package com.fengdis.spider.controller;

import com.fengdis.spider.crawler.FolloweeCrawler;
import com.fengdis.spider.crawler.FollowerCrawler;
import com.fengdis.spider.crawler.UserCrawler;
import com.fengdis.spider.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public void getUserBriefInfo(String urlToken) {
        UserCrawler.getUserInfoFromZhihu(urlToken);
    }

    @GetMapping("/followers")
    public void getUserFollowerInfoList (String urlToken, @RequestParam(required = false) Integer offset) {
        if (offset == null) {
            offset = 0;
        }
        FollowerCrawler.getUserFollowerInfoListFromZhihu(urlToken, offset);
    }

    @GetMapping("/followees")
    public void getUserFolloweeInfoList (String urlToken, @RequestParam(required = false) Integer offset) {
        if (offset == null) {
            offset = 0;
        }
        FolloweeCrawler.getUserFolloweeInfoListFromZhihu(urlToken, offset);
    }

    @GetMapping("/filter")
    public void deleteDuplicateUserList () {
        userService.removeDuplicateUserList();
    }

    @GetMapping("/transfer")
    public void transferDataFromDBToES() {
        userService.transferDataFromDBToES();
    }
}
