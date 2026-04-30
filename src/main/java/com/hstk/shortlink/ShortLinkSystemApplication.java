package com.hstk.shortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ShortLinkSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkSystemApplication.class, args);
    }

}
