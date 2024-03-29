package com.dzdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@MapperScan("com.dzdp.mapper")
@SpringBootApplication
@EnableTransactionManagement
public class DZDianPingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DZDianPingApplication.class, args);
    }
}
