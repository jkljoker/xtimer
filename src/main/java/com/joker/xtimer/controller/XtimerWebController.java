package com.joker.xtimer.controller;

import com.joker.xtimer.dto.TimerDTO;
import com.joker.xtimer.common.response.ResponseEntity;
import com.joker.xtimer.service.XTimerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/xtimer")
@Slf4j
public class XtimerWebController {

    @Resource
    private XTimerService xTimerService;

    @PostMapping(value = "/createTimer")
    public ResponseEntity<Long> createTimer(@RequestBody TimerDTO timerDTO){
        Long timerId = xTimerService.CreateTimer(timerDTO);
        return ResponseEntity.ok(timerId);
    }

    @GetMapping(value = "/enableTimer")
    public ResponseEntity<String> enableTimer(@RequestParam(value = "app") String app,
                                              @RequestParam(value = "timerId") Long timerId,
                                              @RequestHeader MultiValueMap<String, String> headers){
        xTimerService.EnableTimer(app,timerId);
        return ResponseEntity.ok("ok");
    }
    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody String callbackInfo) {
        log.info("CALLBACK:"+callbackInfo);
        // 消息队列发送消息
        return ResponseEntity.ok(
                "ok"
        );
    }
}
