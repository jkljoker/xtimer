package com.joker.xtimer.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RedisBase {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取 Redis 中的值
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取 Redis 键 [{}] 的值失败", key, e);
            return null;
        }
    }

    /**
     * 使用 SETNX 获取分布式锁
     * @param key Redis 键
     * @param value 锁的值
     * @param expireSeconds 锁的过期时间（秒）
     * @return 是否成功获取到锁
     */
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, java.util.concurrent.TimeUnit.SECONDS);
            return success != null && success;
        } catch (Exception e) {
            log.error("尝试获取锁 [{}] 失败", key, e);
            return false;
        }
    }

    /**
     * 执行 Lua 脚本
     * @param redisScript Lua 脚本
     * @param keys 脚本的键列表
     * @param args 脚本的参数
     * @return 执行结果
     */
    public Long executeLua(DefaultRedisScript<Long> redisScript, List<String> keys, String... args) {
        try {
            return redisTemplate.execute(redisScript, keys, (Object[]) args);
        } catch (Exception e) {
            log.error("执行 Lua 脚本失败", e);
            return null;
        }
    }
}
