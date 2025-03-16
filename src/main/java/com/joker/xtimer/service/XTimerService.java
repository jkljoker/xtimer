package com.joker.xtimer.service;

import com.joker.xtimer.dto.TimerDTO;

import java.util.List;

public interface XTimerService {

    Long CreateTimer(TimerDTO timerDTO);

    void DeleteTimer(String app, long id);

    void Update(TimerDTO timerDTO);

    TimerDTO GetTimer(String app, long id);

    void EnableTimer(String app, long id);

    void UnEnableTimer(String app, long id);

    List<TimerDTO> GetAppTimers(String app);
}