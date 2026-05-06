package com.database_migrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DatabaseMigratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseMigratorApplication.class, args);
    }
}
