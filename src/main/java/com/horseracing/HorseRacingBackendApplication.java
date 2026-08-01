package com.horseracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HorseRacingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HorseRacingBackendApplication.class, args);
    }

}
