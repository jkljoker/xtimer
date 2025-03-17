package com.joker.xtimer.service.scheduler;

import com.joker.xtimer.common.conf.SchedulerAppConf;
import com.joker.xtimer.redis.ReentrantDistributeLock;
import com.joker.xtimer.service.trigger.TriggerWorker;
import com.joker.xtimer.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;


@Slf4j
@Component
public class SchedulerTask {

    @Autowired
    ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    SchedulerAppConf schedulerAppConf;

    @Autowired
    TriggerWorker triggerWorker;

    @Async("schedulerPool")
    public void asyncHandleSlice(Date date, int bucketId) {
        log.info("start executeAsync");

        String lockToken = TimerUtils.GetTokenStr();
        //只加锁，不解锁，超时解锁
        //锁的是桶
        boolean ok = reentrantDistributeLock.lock(
                TimerUtils.GetTimeBucketLockKey(date, bucketId),
                lockToken,
                schedulerAppConf.getTryLockSeconds());
        if (!ok) {
            log.info("asyncHandleSlice 获取分布式锁失败");
            return;
        }

        log.info("Get scheduler lock success, key: {}", TimerUtils.GetTimeBucketLockKey(date, bucketId));

        //调用triggerWorker进行处理
        triggerWorker.work(TimerUtils.GetSliceMsgKey(date, bucketId));

        //延长分布式锁的时间，避免重复执行分片
        reentrantDistributeLock.expireLock(
                TimerUtils.GetTimeBucketLockKey(date, bucketId),
                lockToken,
                schedulerAppConf.getSuccessExpireSeconds());
        log.info("end executeAsync");
    }
}
