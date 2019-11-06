package com.myhexin.web.demo.controller;

import com.myhexin.library.distributedratelimiter.limiter.RateLimiter;
import com.myhexin.library.distributedratelimiter.limiter.RateLimiterBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("test")
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RateLimiterBuilderFactory rateLimiterBuilderFactory;


//    @TokenRateLimiter(key = "a",permitsPerSecond = 1,planb = @PlanB)
    @GetMapping("setkey")
    public Object setkey(@RequestParam(name = "key")String key,@RequestParam(name = "value")String value){
        logger.warn("key{},value{}",key,value);
        redisTemplate.opsForValue().set(key,value,1, TimeUnit.MINUTES);
        return true;
    }

    @GetMapping("getkey")
    public Object getkey(@RequestParam(name = "key")String key){
        Object o = redisTemplate.opsForValue().get(key);

        logger.warn("key-{},value->{}",key,o);
        return o;
    }

    @GetMapping("rate")
    public Object ratelimit(int tokens) throws InterruptedException {
        RateLimiterBuilderFactory.RateLimiterBuilder rateLimiterBuilder = rateLimiterBuilderFactory.newInstance();
        RateLimiter rateLimiter = rateLimiterBuilder.setKey("key2").setMaxBurstSeconds(60).setPermitsPerSecond(60).build();
        boolean b = rateLimiter.tryAcquire(tokens);
//        rateLimiter.acquire(1);
//        logger.info("now->{}", LocalDateTime.now().toInstant(ZoneOffset.of("+8")));
        return b;
    }
}
