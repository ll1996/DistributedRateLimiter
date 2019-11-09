package com.myhexin.library.detributedratelimitercore.limiter;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶数据模型
 */
public class RedisPermits implements Serializable {
    //最大存储令牌数
    private long maxPermits;
    //当前存储令牌数
    private long storedPermits;
    //添加令牌时间间隔
    private long intervalMillis;
    //下次请求可以获取令牌的起始时间，默认当前系统时间
    private long nextFreeTicketMillis;

    public RedisPermits(double permitsPerSecond,int maxBurstSeconds){
        this(permitsPerSecond,maxBurstSeconds,System.currentTimeMillis());
    }

    public RedisPermits() {
    }

    public RedisPermits(double permitsPerSecond, int maxBurstSeconds, long nextFreeTicketMillis) {
        this(new Double((permitsPerSecond * maxBurstSeconds)).longValue(), new Double(permitsPerSecond * maxBurstSeconds).longValue(), new Double((TimeUnit.SECONDS.toMillis(1) / permitsPerSecond)).longValue(), nextFreeTicketMillis);
    }

    private RedisPermits(Long maxPermits, Long storedPermits, Long intervalMillis, Long nextFreeTicketMillis) {
        this.maxPermits = maxPermits;
        this.storedPermits = storedPermits;
        this.intervalMillis = intervalMillis;
        this.nextFreeTicketMillis = nextFreeTicketMillis;
    }

    /**
     * 计算redis-key过期时长（秒）
     *
     * @return redis-key过期时长（秒）
     */
    public long expires(){
        Long now = System.currentTimeMillis();
        return 2 * TimeUnit.MINUTES.toSeconds(1) + TimeUnit.MILLISECONDS.toSeconds(Math.max(nextFreeTicketMillis, now) - now);
    }


    /**
     * if nextFreeTicket is in the past, reSync to now
     * 若当前时间晚于nextFreeTicketMicros，则计算该段时间内可以生成多少令牌，将生成的令牌加入令牌桶中并更新数据
     *
     * @return 是否更新
     */
    public boolean reSync(Long now){
        if (now > nextFreeTicketMillis) {
            storedPermits = Math.min(maxPermits, storedPermits + (now - nextFreeTicketMillis) / intervalMillis);
            nextFreeTicketMillis = now;
            return true;
        }
        return false;
    }


    public long getMaxPermits() {
        return maxPermits;
    }

    public void setMaxPermits(long maxPermits) {
        this.maxPermits = maxPermits;
    }

    public long getStoredPermits() {
        return storedPermits;
    }

    public void setStoredPermits(long storedPermits) {
        this.storedPermits = storedPermits;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public long getNextFreeTicketMillis() {
        return nextFreeTicketMillis;
    }

    public void setNextFreeTicketMillis(long nextFreeTicketMillis) {
        this.nextFreeTicketMillis = nextFreeTicketMillis;
    }
}
