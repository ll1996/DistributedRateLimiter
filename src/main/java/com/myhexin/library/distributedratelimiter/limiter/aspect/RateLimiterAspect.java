package com.myhexin.library.distributedratelimiter.limiter.aspect;

import com.myhexin.library.distributedratelimiter.limiter.RateLimiter;
import com.myhexin.library.distributedratelimiter.limiter.RateLimiterBuilderFactory;
import com.myhexin.library.distributedratelimiter.limiter.annotation.TokenRateLimiter;
import com.myhexin.library.distributedratelimiter.limiter.enums.AcquireStrategy;
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

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RateLimiterAspect {
    private static Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);

    @Autowired
    private RateLimiterBuilderFactory rateLimiterBuilderFactory;

    @Pointcut("@annotation(com.myhexin.library.distributedratelimiter.limiter.annotation.TokenRateLimiter)")
    public void pointcut() {
    }

    @Around("pointcut() && @annotation(limiter)")
    public Object around(ProceedingJoinPoint pjp, TokenRateLimiter limiter) throws Throwable {
        Method method = this.resolveMethod(pjp);

        RateLimiter rateLimiter = rateLimiterBuilderFactory
                .newInstance()
                .setKey(limiter.key())
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
            return pjp.proceed();
        }

        if (!limiter.fallBackMethod().isEmpty()) {
            return null;
        }
        Class<?> clazz = limiter.fallBackClass();
        if (clazz.equals(Void.class)) {
            clazz = pjp.getTarget().getClass();
        }
        Object o = clazz.newInstance();
        Method m = clazz.getMethod(limiter.fallBackMethod());
        Object invoke = m.invoke(o);


        Signature sig = pjp.getSignature();
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("This annotation can only be used in methods.");
        }
        return null;

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
