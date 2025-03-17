package com.joker.xtimer.manager;

import com.joker.xtimer.common.conf.MigratorAppConf;
import com.joker.xtimer.enums.TaskStatus;
import com.joker.xtimer.enums.TimerStatus;
import com.joker.xtimer.exception.BusinessException;
import com.joker.xtimer.exception.ErrorCode;
import com.joker.xtimer.mapper.TaskMapper;
import com.joker.xtimer.mapper.TimerMapper;
import com.joker.xtimer.model.TaskModel;
import com.joker.xtimer.model.TimerModel;
import com.joker.xtimer.redis.ReentrantDistributeLock;
import com.joker.xtimer.redis.TaskCache;
import com.joker.xtimer.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Time;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MigrateManagerImpl implements MigratorManager{

    @Autowired
    private TimerMapper timerMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    private MigratorAppConf migratorAppConf;

    @Autowired
    private TaskCache taskCache;

    @Override
    public void migrateTimer(TimerModel timerModel) {
        //校验状态
        if (timerModel.getStatus() != TimerStatus.Enable.getStatus()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Timer非Enable状态，迁移失败，timerId:"+ timerModel.getTimerId());
        }
        //取得批量执行的时机
        CronExpression cronExpression;
        try {
            cronExpression = new CronExpression(timerModel.getCron());
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"解析cron表达式失败："+timerModel.getCron());
        }
        Date now = new Date();
        Date end = TimerUtils.GetForwardTwoMigrateStepEnd(now, migratorAppConf.getMigrateStepMinutes());

        List<Long> executeTimes = TimerUtils.GetCronNextsBetween(cronExpression, now, end);
        if (CollectionUtils.isEmpty(executeTimes)) {
            log.warn("获取的执行时机 executeTimes 为空");
            return;
        }
        //执行时机加入数据库
        List<TaskModel> taskList = batchTasksFromTimer(timerModel, executeTimes);
        //插入数据库
        taskMapper.batchSave(taskList);
        //加入redis Zset
        boolean cacheRes = taskCache.cacheSaveTasks(taskList);
        if (!cacheRes) {
            log.error("Zset存储taskList失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ZSet存储taskList失败，timerId:"+timerModel.getTimerId());
        }
    }

    private List<TaskModel> batchTasksFromTimer(TimerModel timerModel, List<Long> executeTimes) {
        if (timerModel== null || CollectionUtils.isEmpty(executeTimes)) {
            return null;
        }

        List<TaskModel> taskList = new ArrayList<>();
        for (Long runTimer:executeTimes) {
            TaskModel task = new TaskModel();
            task.setApp(timerModel.getApp());
            task.setTimerId(timerModel.getTimerId());
            task.setRunTimer(runTimer);
            task.setStatus(TaskStatus.NotRun.getStatus());
            taskList.add(task);
        }
        return taskList;
    }
}
