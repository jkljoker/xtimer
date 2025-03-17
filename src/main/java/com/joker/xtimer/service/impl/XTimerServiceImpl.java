package com.joker.xtimer.service.impl;

import com.joker.xtimer.dto.TimerDTO;
import com.joker.xtimer.enums.TimerStatus;
import com.joker.xtimer.exception.BusinessException;
import com.joker.xtimer.exception.ErrorCode;
import com.joker.xtimer.manager.MigratorManager;
import com.joker.xtimer.mapper.TimerMapper;
import com.joker.xtimer.model.TimerModel;
import com.joker.xtimer.redis.ReentrantDistributeLock;
import com.joker.xtimer.service.XTimerService;
import com.joker.xtimer.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class XTimerServiceImpl implements XTimerService {
    @Autowired
    private TimerMapper timerMapper;

    @Autowired
    ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    MigratorManager migratorManager;

    private static final int  defaultGapSeconds= 3;

    @Override
    public Long CreateTimer(TimerDTO timerDTO) {
        boolean isValidCron = CronExpression.isValidExpression(timerDTO.getCron());
        if (!isValidCron) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid cron");
        }

        TimerModel timerModel = TimerModel.voToObj(timerDTO);
        if (timerModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        timerMapper.save(timerModel);
        return timerModel.getTimerId();
    }

    @Override
    public void DeleteTimer(String app, long id) {
        String lockToken = TimerUtils.GetTokenStr();
        boolean ok = reentrantDistributeLock.lock(
                TimerUtils.GetCreateLockKey(app),
                lockToken,
                defaultGapSeconds);

        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建/删除操作过于频繁，请稍后再试！");
        }

        timerMapper.deleteById(id);
    }


    @Override
    public void Update(TimerDTO timerDTO) {
        TimerModel timerModel = TimerModel.voToObj(timerDTO);
        if (timerModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        timerMapper.update(timerModel);
    }

    @Override
    public TimerDTO GetTimer(String app, long id) {
        TimerModel timerModel = timerMapper.getTimerById(id);
        TimerDTO timerDTO = TimerModel.objToVo(timerModel);
        return timerDTO;
    }

    @Override
    public void EnableTimer(String app, long id) {
        String lockToken = TimerUtils.GetTokenStr();
        boolean ok = reentrantDistributeLock.lock(
                TimerUtils.GetEnableLockKey(app),
                lockToken,
                defaultGapSeconds);

        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "激活/去激活操作过于频繁，请稍后再试！");
        }

        doEnableTimer(id);
    }

    @Transactional
    public void doEnableTimer(long id) {
        TimerModel timerModel = timerMapper.getTimerById(id);
        if (timerModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "激活失败，timer不存在：timerId" + id);
        }

        if (timerModel.getStatus() == TimerStatus.Enable.getStatus()) {
            log.warn("Timer非Unable状态，激活失败，timerId:" + timerModel.getTimerId());
        }

        timerModel.setStatus(TimerStatus.Enable.getStatus());
        timerMapper.update(timerModel);

        // 迁移数据
        migratorManager.migrateTimer(timerModel);
    }

    @Override
    public void UnEnableTimer(String app, long id) {
        String lockToken = TimerUtils.GetTokenStr();
        boolean ok = reentrantDistributeLock.lock(
                TimerUtils.GetEnableLockKey(app),
                lockToken,
                defaultGapSeconds);

        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "激活/去激活操作过于频繁，请稍后再试！");
        }

        doUnEnableTimer(id);
    }

    @Override
    public List<TimerDTO> GetAppTimers(String app) {
        return null;
    }

    @Transactional
    public void doUnEnableTimer(Long id) {
        TimerModel timerModel = timerMapper.getTimerById(id);
        if (timerModel.getStatus() != TimerStatus.Unable.getStatus()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Timer非Unable状态，去激活失败，id:" + id);
        }
        timerModel.setStatus(TimerStatus.Unable.getStatus());
        timerMapper.update(timerModel);
    }

}
