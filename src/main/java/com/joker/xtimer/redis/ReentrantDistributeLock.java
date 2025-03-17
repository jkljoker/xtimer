package com.joker.xtimer.redis;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;

@Component
@Slf4j
public class ReentrantDistributeLock {

    @Autowired
    private RedisBase redisBase;

    @PostConstruct
    public void init() {
        if (redisBase == null) {
            log.error("RedisBase 未成功注入，分布式锁功能可能无法正常使用！");
        }
    }

    /**
     * 获取锁（可重入）
     */
    public boolean lock(String key, String token, long expireSeconds) {
        try {
            // 先检查当前锁是否属于自己
            Object res = redisBase.get(key);
            if (res != null && StringUtils.equals(res.toString(), token)) {
                return true;
            }

            // 尝试获取锁
            boolean acquired = redisBase.setIfAbsent(key, token, expireSeconds);
            if (!acquired) {
                log.info("锁 [{}] 已被其他线程占用", key);
            }
            return acquired;
        } catch (Exception e) {
            log.error("获取锁 [{}] 失败", key, e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key, String token) {
        try {
            Long result = redisBase.executeLua(getUnlockScript(), Collections.singletonList(key), token);
            if (result != null && result == 1) {
                log.info("释放锁 [{}] 成功", key);
            } else {
                log.warn("释放锁 [{}] 失败，锁可能已不存在或不属于当前线程", key);
            }
        } catch (Exception e) {
            log.error("释放锁 [{}] 异常", key, e);
        }
    }

    /**
     * 续期锁
     */
    public void expireLock(String key, String token, long expireSeconds) {
        try {
            Long result = redisBase.executeLua(getExpireLockScript(), Collections.singletonList(key), token, String.valueOf(expireSeconds));
            if (result != null && result == 1) {
                log.info("锁 [{}] 续期成功", key);
            } else {
                log.warn("锁 [{}] 续期失败，锁可能已失效或不属于当前线程", key);
            }
        } catch (Exception e) {
            log.error("锁 [{}] 续期异常", key, e);
        }
    }

    /**
     * 获取释放锁的 Lua 脚本
     */
    private DefaultRedisScript<Long> getUnlockScript() {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText(script);
        return redisScript;
    }

    /**
     * 获取续期锁的 Lua 脚本
     */
    private DefaultRedisScript<Long> getExpireLockScript() {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText(script);
        return redisScript;
    }
}
