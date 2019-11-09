package com.myhexin.library.detributedratelimitercore.limiter.annotation;


import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommonRateLimiter {

    //限流的唯一标识，多个接口使用一个限流规则表示共同使用一份资源
    String key();
    //每秒生成令牌数
    double permitsPerSecond();
    //最多保存多少秒的令牌
    int maxBurstSeconds() default 60;
}
