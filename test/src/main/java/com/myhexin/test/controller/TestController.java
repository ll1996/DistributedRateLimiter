package com.myhexin.test.controller;

import com.myhexin.library.detributedratelimitercore.limiter.RateLimiter;
import com.myhexin.library.detributedratelimitercore.limiter.RateLimiterBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("test")
public class TestController {

    @Autowired
    private RateLimiterBuilderFactory rateLimiterBuilderFactory;

    @GetMapping("t1")
    public Object test1(){
        RateLimiterBuilderFactory.RateLimiterBuilder rateLimiterBuilder = rateLimiterBuilderFactory.newInstance();
        RateLimiter rateLimiter = rateLimiterBuilder.setKey("aaaa").setMaxBurstSeconds(5).setPermitsPerSecond(1).build();
        boolean acquire = rateLimiter.tryAcquire();
        System.out.println(acquire);
        return acquire;
    }

}
