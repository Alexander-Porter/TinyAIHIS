package com.tinyhis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TinyHIS - Lightweight Hospital Information System
 * Main Application Entry Point
 */
@SpringBootApplication
@MapperScan("com.tinyhis.mapper")
public class TinyHISApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyHISApplication.class, args);
    }
}
