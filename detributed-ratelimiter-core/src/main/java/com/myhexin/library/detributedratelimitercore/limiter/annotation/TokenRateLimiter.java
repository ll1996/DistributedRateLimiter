package com.myhexin.library.detributedratelimitercore.limiter.annotation;

import com.myhexin.library.detributedratelimitercore.limiter.enums.AcquireStrategy;


public @interface TokenRateLimiter {
    //限流的唯一标识，多个接口使用一个限流规则表示共同使用一份资源
    String key();
    //每秒生成令牌数
    double permitsPerSecond();
    //最多保存多少秒的令牌
    int maxBurstSeconds() default 60;
    //超时时间(秒)
    int timeout() default 3;
    //消费令牌数量
    int consume() default 1;
    //被限制后的策略
    AcquireStrategy strategy() default AcquireStrategy.FAILFAST;

    Class<?> fallBackClass() default Void.class;

    String fallBackMethod() default "";

}
