package com.myhexin.library.detributedratelimitercore.limiter;


import com.google.common.base.Preconditions;
import com.myhexin.library.detributedratelimitercore.lock.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimiterBuilderFactory {

    private ConcurrentMap<String,RateLimiter> ratelimiterMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisLock redisLock;

    public RateLimiterBuilder newInstance(){
        return new RateLimiterBuilder(redisTemplate,redisLock);
    }

    public class RateLimiterBuilder{
        private RedisTemplate redisTemplate;
        private RedisLock redisLock;

        private String key;
        private double permitsPerSecond;
        private int maxBurstSeconds;

        public RateLimiterBuilder setKey(String key){
            this.key=key;
            return this;
        }

        public String getKey() {
            return key;
        }

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public RateLimiterBuilder setPermitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
            return this;
        }

        public int getMaxBurstSeconds() {
            return maxBurstSeconds;
        }

        public RateLimiterBuilder setMaxBurstSeconds(int maxBurstSeconds) {
            this.maxBurstSeconds = maxBurstSeconds;
            return this;
        }

        public RateLimiterBuilder(RedisTemplate redisTemplate, RedisLock redisLock) {
            this.redisTemplate = redisTemplate;
            this.redisLock = redisLock;
        }

        public RateLimiter build(){
            Preconditions.checkNotNull(key,"限流属性[key]未添加");
            Preconditions.checkNotNull(permitsPerSecond,"限流属性[permitsPerSecond]每秒令牌数未添加");
            Preconditions.checkNotNull(maxBurstSeconds,"限流属性[maxBurstSeconds]令牌桶大小未添加");
            RateLimiter rateLimiter = ratelimiterMap.get(key);
            if (Objects.isNull(rateLimiter)) {
                rateLimiter = new RateLimiter(key, permitsPerSecond, maxBurstSeconds, redisTemplate, redisLock);
                ratelimiterMap.put(key,rateLimiter);
            }
            return rateLimiter;
        }

    }


}
