package com.db_migrator.etl_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EtlSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlSystemApplication.class, args);
    }
}
