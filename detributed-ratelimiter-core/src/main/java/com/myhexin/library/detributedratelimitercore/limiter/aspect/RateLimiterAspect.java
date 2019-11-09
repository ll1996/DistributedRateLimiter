package com.myhexin.library.detributedratelimitercore.limiter.aspect;

import com.myhexin.library.detributedratelimitercore.limiter.RateLimiter;
import com.myhexin.library.detributedratelimitercore.limiter.RateLimiterBuilderFactory;
import com.myhexin.library.detributedratelimitercore.limiter.annotation.TokenRateLimiter;
import com.myhexin.library.detributedratelimitercore.limiter.enums.AcquireStrategy;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RateLimiterAspect {
    private static Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);

    @Autowired
    private RateLimiterBuilderFactory rateLimiterBuilderFactory;

    @Pointcut("@annotation(com.myhexin.library.detributedratelimitercore.limiter.annotation.TokenRateLimiter)")
    public void pointcut() {
    }

    @Around("pointcut() && @annotation(limiter)")
    public Object around(ProceedingJoinPoint pjp, TokenRateLimiter limiter) throws Throwable {
        Method method = this.resolveMethod(pjp);

        String key = limiter.key();
        if(StringUtils.isEmpty(key)){
            key = pjp.getSignature().toLongString();
        }
        if (limiter.isBasedIP()) {

        }
        RateLimiter rateLimiter = rateLimiterBuilderFactory
                .newInstance()
                .setKey(key)
                .setMaxBurstSeconds(limiter.maxBurstSeconds())
                .setPermitsPerSecond(limiter.permitsPerSecond())
                .build();

        boolean isAcquire=false;
        if(limiter.strategy().equals(AcquireStrategy.FAILFAST)){
            isAcquire = rateLimiter.tryAcquire(limiter.consume());
        }else{
            isAcquire = rateLimiter.tryAcquire(limiter.consume(),limiter.timeout(),TimeUnit.SECONDS);
        }
        if(isAcquire){
            logger.info("1");
            return pjp.proceed();
        }
        if (limiter.fallBackMethod().isEmpty()) {
            return "current access is limited, please wait and refresh";
        }
        Class<?> clazz = limiter.fallBackClass();
        if (clazz.equals(Void.class)) {
            clazz = pjp.getTarget().getClass();
        }
        Object o = clazz.newInstance();
        Method m = clazz.getDeclaredMethod(limiter.fallBackMethod());
        m.setAccessible(true);
        Object invoke = m.invoke(o);
        return invoke;

//        Signature sig = pjp.getSignature();
//        if (!(sig instanceof MethodSignature)) {
//            throw new IllegalArgumentException("This annotation can only be used in methods.");
//        }
//        return null;

    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = this.getDeclaredMethodFor(targetClass, signature.getName(), signature.getMethod().getParameterTypes());
        if (method == null) {
            throw new IllegalStateException("Cannot resolve target method: " + signature.getMethod().getName());
        } else {
            return method;
        }
    }
    private Method getDeclaredMethodFor(Class<?> clazz, String name, Class... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException var6) {
            Class<?> superClass = clazz.getSuperclass();
            return superClass != null ? this.getDeclaredMethodFor(superClass, name, parameterTypes) : null;
        }
    }


}
