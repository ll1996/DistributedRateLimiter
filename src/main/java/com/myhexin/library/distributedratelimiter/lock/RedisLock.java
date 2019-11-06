package com.myhexin.library.distributedratelimiter.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String getLockScript = "if redis.call('setNx',KEYS[1],ARGV[1])==1 then \n return redis.call('expire',KEYS[1],ARGV[2])) \n else \n return 0 \n end ";
    private static final String releaseLockScript = "if redis.call('get',KEYS[1])==ARGV[1] then \n return redis.call('del',KEYS[1]) \n else \n return 0 \n end ";

    /**
     * 获取锁
     * @param lockKey   key
     * @param value     value
     * @param expire    有效时长(秒)
     * @return
     */
    public boolean tryLock(String lockKey,String value, int expire){
        RedisScript<Long> redisScript = new DefaultRedisScript<>(getLockScript,Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), value, String.valueOf(expire));
        return result.equals(1);
    }

    /**
     *
     * @param lockKey 抢占锁的key
     * @param value   抢占所的value，只有当key和value一致才能解锁
     * @param timeout 等待超时时间(秒)
     * @return
     */
    public boolean tryLock(String lockKey,String value,int expire,int timeout) throws InterruptedException {
        timeout*=1000;
        long waitAlready = 0;
        long waitMillisPer = 10;
        while (stringRedisTemplate.opsForValue().setIfAbsent(lockKey, value) != true && waitAlready < timeout) {
            Thread.sleep(waitMillisPer);
            waitAlready += waitMillisPer;
        }
        if (waitAlready < timeout) {
            stringRedisTemplate.expire(lockKey, expire, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    /**
     * 设置安全锁,超出等待时间无论成功不成功自动将值设置进去,确保不会因为意外原因导致锁释放不了
     * @param lockKey               key
     * @param value                 value
     * @param expire                缓存时长(秒)
     * @param safeLockLockTime      安全锁定时长(秒)
     * @throws InterruptedException
     */
    public void lock(String lockKey,String value,int expire,int safeLockLockTime) throws InterruptedException {
        safeLockLockTime*=1000;
        long waitAlready = 0;
        long waitMillisPer = 10;
        while (stringRedisTemplate.opsForValue().setIfAbsent(lockKey, value) != true && waitAlready < safeLockLockTime) {
            Thread.sleep(waitMillisPer);
            waitAlready += waitMillisPer;
        }

        stringRedisTemplate.opsForValue().set(lockKey, value, expire, TimeUnit.SECONDS);
    }


    /**
     * 释放锁
     * @param lockKey
     * @param value
     * @return
     */
    public boolean releaseLock(String lockKey,String value){
        RedisScript<Long> redisScript = new DefaultRedisScript<>(releaseLockScript,Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), value);
        return result.equals(1);
    }


}
