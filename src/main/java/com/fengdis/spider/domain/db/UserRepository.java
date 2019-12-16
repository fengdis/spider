package com.fengdis.spider.domain.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUrlToken(String urlToken);

    @Query(nativeQuery = true, value = "select id from user order by id desc limit 1")
    Long selectLargestId();

    @Query(nativeQuery = true, value = "select * from user where id >=  ?1 and id <= ?2")
    List<User> findIdBetween(Long start, Long end);

    @Query(nativeQuery = true, value = "select * from user order by id limit ?1, ?2")
    List<User> findByPageQuery(Integer offset, Integer pageSize);
}
