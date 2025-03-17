package com.joker.xtimer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.joker"})
@EnableScheduling
@EnableAsync
@MapperScan("com.joker.xtimer.mapper")
public class XtimerApplication {

    public static void main(String[] args) {
        SpringApplication.run(XtimerApplication.class, args);
    }

}
