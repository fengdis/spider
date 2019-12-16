package com.fengdis.spider.service;

import com.fengdis.spider.common.ProxyIpStatus;
import com.fengdis.spider.common.RedisConstants;
import com.fengdis.spider.domain.db.IpProxy;
import com.fengdis.spider.domain.db.IpProxyRepository;
import com.fengdis.spider.util.HttpUtil;
import com.fengdis.spider.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class IpProxyService {

    @Autowired
    private IpProxyRepository ipProxyRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void checkProxyIpAndSaveToDB(IpProxy ipProxy) {
        List<IpProxy> isExistedIpProxyList = ipProxyRepository.findByIpAndPort(ipProxy.getIp(), ipProxy.getPort());
        Integer preSuccessTimes = 0, preFailTimes = 0;
        if (isExistedIpProxyList.size() > 0) {
            ipProxy = isExistedIpProxyList.get(0);
            preSuccessTimes = ipProxy.getSuccessTimes();
            preFailTimes = ipProxy.getFailTimes();
        }
        boolean isActive = HttpUtil.checkIpIsActive(ipProxy.getIp(), Integer.valueOf(ipProxy.getPort()), Proxy.Type.HTTP);
        log.info("[将代理保存进DB]验证该代理{}:{},是否可用:{}", ipProxy.getIp(), ipProxy.getPort(), isActive);
        if (isActive) {
            ipProxy.setStatus(ProxyIpStatus.SUCCESS.getValue());
            ipProxy.setSuccessTimes(++preSuccessTimes);
            ipProxy.setFailTimes(0);
            ipProxyRepository.save(ipProxy);
        } else if (preFailTimes < 2) {
            ipProxy.setStatus(ProxyIpStatus.FAIL.getValue());
            ipProxy.setSuccessTimes(0);
            ipProxy.setFailTimes(++preFailTimes);
            ipProxyRepository.save(ipProxy);
        } else {
            ipProxyRepository.delete(ipProxy);
        }
    }

    /**
     * 获取一条可用的HTTP代理
     * @return 可用的代理
     */
    public IpProxy getActiveProxyIp() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisConstants.PROXY_IP_HASH_NAME);
        List ipProxyList = new ArrayList<>(entries.values());
        if (entries.size() == 0) {
            return null;
        } else {
            Random random = new Random();
            int index = random.nextInt(ipProxyList.size());
            return JsonUtil.string2Obj((String) ipProxyList.get(index), IpProxy.class);
        }
    }
}
