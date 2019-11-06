package com.myhexin.library.distributedratelimiter.limiter;

import com.google.common.base.Preconditions;
import com.google.common.math.LongMath;
import com.myhexin.library.distributedratelimiter.lock.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    private String key;
    private double permitsPerSecond;
    private int maxBurstSeconds;
    private RedisTemplate<String,Object> redisTemplate;
    private RedisLock redisLock;



    /**
     * constructor
     * @param key
     * @param permitsPerSecond
     * @param maxBurstSeconds
     * @param redisTemplate
     * @param redisLock
     */
    public RateLimiter(String key, double permitsPerSecond, int maxBurstSeconds, RedisTemplate<String, Object> redisTemplate, RedisLock redisLock) {
        this.key = key;
        this.permitsPerSecond = permitsPerSecond;
        this.maxBurstSeconds = maxBurstSeconds;
        this.redisTemplate = redisTemplate;
        this.redisLock = redisLock;
    }


    public long acquire(long tokens) throws InterruptedException {
        long milliToWait = reserve(tokens);
        logger.debug("acquire for {}ms {}", milliToWait, Thread.currentThread().getName());
        Thread.sleep(milliToWait);
        return milliToWait;
    }

    public boolean tryAcquire(Duration timeout) {
        return this.tryAcquire(1, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return this.tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int permits) {
        return this.tryAcquire(permits, 0L, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire() {
        return this.tryAcquire(1, 0L, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire(int permits, Duration timeout) {
        return this.tryAcquire(permits, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }


    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = Math.max(unit.toMicros(timeout), 0L);
        checkTokens(permits);
        long microsToWait;
        String uuid = UUID.randomUUID().toString();
        try {
            redisLock.lock("$ratelimiter$",uuid,10,10);
            if (!this.canAcquire(permits, timeoutMicros)) {
                return false;
            }
            microsToWait = this.reserveAndGetWaitLength(permits);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            redisLock.releaseLock("$ratelimiter$",uuid);
        }
        if(microsToWait>0){
            try {
                Thread.sleep(microsToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 判断时候能够在超时时间内访问到资源
     * @param tokens            消费的令牌数
     * @param timeoutMillis     超时时间
     * @return
     */
    private boolean canAcquire(long tokens, long timeoutMillis ) {
        return queryEarliestAvailable(tokens) - timeoutMillis <= 0;
    }



    /**
     * 用于计算，获取tokens个令牌需要等待的时长（毫秒）
     * @param tokens    令牌数
     * @return          需要等待的毫秒数
     */
    private long queryEarliestAvailable(long tokens) {
        long n = getNow();
        RedisPermits permit = getPermits();
        permit.reSync(n);
        long storedPermitsToSpend = Math.min(tokens, permit.getStoredPermits()); // 可以消耗的令牌数
        long freshPermits = tokens - storedPermitsToSpend; // 需要等待的令牌数
        long waitMillis = freshPermits * permit.getIntervalMillis(); // 需要等待的时间

        return LongMath.saturatedAdd(permit.getNextFreeTicketMillis() - n, waitMillis);
    }


    /**
     * 改善Guava消费行为,为自己的行为买单
     * @param tokens
     * @return
     * @throws InterruptedException
     */
    public long reserve(long tokens) throws InterruptedException {
        checkTokens(tokens);
        String uuid = UUID.randomUUID().toString();
        try {
            redisLock.lock("$ratelimiter$",uuid,10,10);
            return reserveAndGetWaitLength(tokens);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw e;
        } finally {
            redisLock.releaseLock("$ratelimiter$",uuid);
        }
    }


    /**
     * 该函数用于获取tokens个令牌，并返回需要等待到的时长（毫秒）
     * @param tokens
     * @return
     */
    private long reserveAndGetWaitLength(long tokens) {
        long n = getNow();
        RedisPermits permit = getPermits();
        permit.reSync(n);
        long storedPermitsToSpend = Math.min(tokens, permit.getStoredPermits()); // 可以消耗的令牌数
        long freshPermits = tokens - storedPermitsToSpend; // 需要等待的令牌数
        long waitMillis = freshPermits * permit.getIntervalMillis(); // 需要等待的时间

        permit.setNextFreeTicketMillis( LongMath.saturatedAdd(permit.getNextFreeTicketMillis(), waitMillis));
        permit.setStoredPermits(permit.getStoredPermits()-storedPermitsToSpend);
        setPermits(permit);

        return permit.getNextFreeTicketMillis() - n;
    }



    /**
     * 检查令牌数量
     * @param tokens
     */
    private void checkTokens(long tokens) {
        Preconditions.checkArgument(tokens > 0, "Requested tokens $tokens must be positive");
    }

    /**
     * 获取令牌桶
     * @return
     */
    public RedisPermits getPermits(){
        RedisPermits redisPermits = (RedisPermits) redisTemplate.opsForValue().get(key);
        if(Objects.isNull(redisPermits)){
            redisPermits = putDefaultPermits();
        }
        return redisPermits;
    }

    /**
     * 生成并存储默认令牌桶
     * @return
     */
    public RedisPermits putDefaultPermits(){
        RedisPermits redisPermits = new RedisPermits(permitsPerSecond,maxBurstSeconds);
        redisTemplate.opsForValue().set(key,redisPermits,redisPermits.expires(), TimeUnit.SECONDS);
        return redisPermits;
    }

    /**
     * 更新令牌数
     * @param redisPermits
     */
    public void setPermits(RedisPermits redisPermits){
        redisTemplate.opsForValue().set(key, redisPermits, redisPermits.expires(), TimeUnit.SECONDS);
    }

    public long getNow(){
//        redisTemplate.execute;
        return System.currentTimeMillis();
    }

}
