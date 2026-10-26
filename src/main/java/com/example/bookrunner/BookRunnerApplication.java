package com.example.bookrunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookRunnerApplication.class, args);
    }

}
