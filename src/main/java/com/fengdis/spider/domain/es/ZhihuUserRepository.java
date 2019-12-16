package com.fengdis.spider.domain.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZhihuUserRepository extends ElasticsearchRepository<ZhihuUser, Long> {
}
