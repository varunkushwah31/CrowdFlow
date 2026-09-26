package com.civic.waterwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class WaterWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterWatchApplication.class, args);
    }
}
