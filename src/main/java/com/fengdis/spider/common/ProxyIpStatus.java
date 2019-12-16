package com.fengdis.spider.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProxyIpStatus {
    SUCCESS("检测成功",1),
    FAIL("检测失败", 0),
    TO_TEST("待检测", -1);
    private String desc;
    private Integer value;

}
