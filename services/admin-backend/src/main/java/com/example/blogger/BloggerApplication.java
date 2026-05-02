package com.example.blogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.blogger.mapper")
public class BloggerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BloggerApplication.class, args);
    }
}
