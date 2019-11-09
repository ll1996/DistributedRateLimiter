package com.myhexin.library.detributedratelimitercore.limiter.annotation;

import com.myhexin.library.detributedratelimitercore.limiter.enums.AcquireStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TokenRateLimiter {
    //限流的唯一标识
    //多个接口使用一个限流规则表示共同使用一份资源,默认为所在方法类名+方法名
    String key() default "";
    //是否基于IP限流
    //适用于一些固定资源或者防止恶意刷接口
    boolean isBasedIP() default false;
    //每秒生成令牌数
    double permitsPerSecond();
    //最多保存多少秒的令牌
    int maxBurstSeconds() default 1;
    //超时时间(秒)
    int timeout() default 3;
    //消费令牌数量
    int consume() default 1;
    //被限制后的策略
    AcquireStrategy strategy() default AcquireStrategy.FAILFAST;
    //备用方法所在的类，如果不传入则为当前方法所在类
    Class<?> fallBackClass() default Void.class;
    //备用方法名
    String fallBackMethod() default "";

}
